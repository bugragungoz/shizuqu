package moe.shizuku.manager.starter

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import moe.shizuku.manager.MainActivity
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbClient
import moe.shizuku.manager.adb.AdbKey
import moe.shizuku.manager.adb.AdbMdns
import moe.shizuku.manager.adb.PreferenceAdbKeyStore
import rikka.shizuku.Shizuku

/** Entry points for [BootStartService], callable on every API level. */
object BootStart {

    private const val TAG = "BootStart"

    /** Android 13+ and the WRITE_SECURE_SETTINGS grant the server gives the manager. */
    fun canStart(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    /** @return false if the system refused to start the service */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun start(context: Context): Boolean {
        return try {
            context.startForegroundService(Intent(context, BootStartService::class.java))
            true
        } catch (e: Throwable) {
            Log.w(TAG, "can't start BootStartService", e)
            false
        }
    }
}

/**
 * Starts the service through wireless debugging without root: after a reboot
 * ("Start on boot") and from the Quick Settings tile ([StartTileService]).
 *
 * Upstream did this inside the boot receiver with a 3 second window, which
 * usually ends before Wi-Fi is connected, so it rarely worked. This short
 * foreground service waits for Wi-Fi (adbd only enables wireless debugging on
 * a network the user allowed before), turns wireless debugging on, finds the
 * TLS port over mDNS and runs the starter, retrying a few times.
 *
 * A short service may run for about 3 minutes; everything here fits in
 * [RUN_BUDGET_MS], and [onTimeout] stops it cleanly if the system ends it
 * earlier.
 *
 * Only runs when the user enabled "Start on boot" (the receiver is disabled by
 * default). Needs Android 13+ and WRITE_SECURE_SETTINGS, which the server
 * grants to the manager the first time it starts.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class BootStartService : Service() {

    companion object {

        private const val TAG = "BootStartService"
        private const val CHANNEL = "boot_start"
        private const val NOTIFICATION_ID = 3
        private const val RESULT_NOTIFICATION_ID = 4

        private const val RUN_BUDGET_MS = 160_000L
        private const val WIFI_TIMEOUT_MS = 90_000L
        private const val DISCOVERY_TIMEOUT_MS = 20_000L
        private const val READ_TIMEOUT_MS = 15_000
        private const val BINDER_TIMEOUT_MS = 10_000L
        private const val MAX_ATTEMPTS = 3
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // only touched on the main thread
    private var job: Job? = null

    // the newest start request; stopSelfResult(lastStartId) does not stop the
    // service if a newer startForegroundService() is still being delivered
    @Volatile
    private var lastStartId = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        createChannel()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, progressNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
            } else {
                // Android 13: foreground service types are not required yet
                startForeground(NOTIFICATION_ID, progressNotification())
            }
        } catch (e: Throwable) {
            Log.w(TAG, "startForeground failed", e)
            // a running attempt keeps going (and stops the service when it is done)
            if (job?.isActive != true) stopSelfResult(startId)
            return START_NOT_STICKY
        }

        // LOCKED_BOOT_COMPLETED and BOOT_COMPLETED both start the service;
        // one attempt covers both
        if (job?.isActive == true) return START_NOT_STICKY

        job = scope.launch {
            val started = try {
                startService()
            } catch (e: CancellationException) {
                // onTimeout or onDestroy: they clean up themselves
                throw e
            } catch (e: Throwable) {
                Log.w(TAG, "start on boot failed", e)
                false
            }
            // on the main thread, like onStartCommand, so the two never interleave
            withContext(Dispatchers.Main) { finish(started) }
        }
        return START_NOT_STICKY
    }

    private suspend fun startService(): Boolean {
        val deadline = SystemClock.elapsedRealtime() + RUN_BUDGET_MS
        if (Shizuku.pingBinder()) return true

        if (!waitForWifi()) {
            Log.w(TAG, "no Wi-Fi within ${WIFI_TIMEOUT_MS / 1000}s")
            return false
        }

        val cr = contentResolver
        for (attempt in 1..MAX_ATTEMPTS) {
            val remaining = deadline - SystemClock.elapsedRealtime()
            if (remaining < DISCOVERY_TIMEOUT_MS + BINDER_TIMEOUT_MS) {
                Log.w(TAG, "out of time after ${attempt - 1} attempt(s)")
                break
            }

            // adbd turns wireless debugging off again on networks the user did
            // not allow, so set it on every attempt
            putSetting { Settings.Global.putInt(cr, "adb_wifi_enabled", 1) }
            putSetting { Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1) }
            putSetting { Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L) }

            val port = discoverPort()
            Log.i(TAG, "attempt $attempt: adb tls port $port")
            if (port > 0 && runStarter(port)) {
                return true
            }
            if (Shizuku.pingBinder()) return true
            delay(3_000)
        }
        return Shizuku.pingBinder()
    }

    private inline fun putSetting(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            Log.w(TAG, "can't write setting", e)
        }
    }

    private suspend fun waitForWifi(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return true
        return withTimeoutOrNull(WIFI_TIMEOUT_MS) {
            while (!isWifiConnected(cm)) delay(2_000)
            true
        } ?: false
    }

    // Any connected Wi-Fi counts, not only the default network: a Wi-Fi
    // without internet access is often not the default but adbd works on it.
    @Suppress("DEPRECATION")
    private fun isWifiConnected(cm: ConnectivityManager): Boolean {
        return cm.allNetworks.any { network ->
            cm.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
    }

    private suspend fun discoverPort(): Int {
        val result = CompletableDeferred<Int>()
        val mdns = AdbMdns(this, AdbMdns.TLS_CONNECT) { port ->
            if (port in 1..65535) result.complete(port)
        }
        mdns.start()
        return try {
            withTimeoutOrNull(DISCOVERY_TIMEOUT_MS) { result.await() } ?: -1
        } finally {
            mdns.stop()
        }
    }

    private suspend fun runStarter(port: Int): Boolean {
        try {
            val key = AdbKey(PreferenceAdbKeyStore(ShizukuSettings.getPreferences()), "shizuku")
            AdbClient("127.0.0.1", port, key, READ_TIMEOUT_MS).use { client ->
                client.connect()
                client.shellCommand(Starter.internalCommand, null)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "adb start failed", e)
            return false
        }
        return withTimeoutOrNull(BINDER_TIMEOUT_MS) {
            while (!Shizuku.pingBinder()) delay(500)
            true
        } ?: false
    }

    // main thread
    private fun finish(started: Boolean) {
        Log.i(TAG, "finished, started=$started")
        // the attempt is over: a start request delivered after this launches a new one
        // (its coroutine may still look active for a moment)
        job = null
        if (!started) notifyFailed()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelfResult(lastStartId)
    }

    private fun notifyFailed() {
        getSystemService(NotificationManager::class.java)
            .notify(RESULT_NOTIFICATION_ID, failedNotification())
    }

    // Android 14+: the system ends a short service after about 3 minutes and
    // requires it to stop right away
    override fun onTimeout(startId: Int) {
        Log.w(TAG, "short service timeout")
        job?.cancel()
        if (!Shizuku.pingBinder()) notifyFailed()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        onTimeout(startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.notification_channel_boot_start), NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            })
    }

    private fun progressNotification(): Notification {
        return Notification.Builder(this, CHANNEL)
            .setColor(getColor(R.color.notification))
            .setSmallIcon(R.drawable.ic_system_icon)
            .setContentTitle(getString(R.string.notification_boot_start_title))
            .setContentText(getString(R.string.notification_boot_start_text))
            .setOngoing(true)
            .build()
    }

    private fun failedNotification(): Notification {
        val intent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL)
            .setColor(getColor(R.color.notification))
            .setSmallIcon(R.drawable.ic_system_icon)
            .setContentTitle(getString(R.string.notification_boot_start_failed_title))
            .setContentText(getString(R.string.notification_boot_start_failed_text))
            .setStyle(Notification.BigTextStyle().bigText(getString(R.string.notification_boot_start_failed_text)))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
    }
}

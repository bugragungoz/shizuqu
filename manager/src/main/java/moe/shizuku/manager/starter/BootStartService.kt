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
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import moe.shizuku.manager.MainActivity
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbClient
import moe.shizuku.manager.adb.AdbKey
import moe.shizuku.manager.adb.AdbMdns
import moe.shizuku.manager.adb.PreferenceAdbKeyStore
import rikka.shizuku.Shizuku
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Starts the service through wireless debugging after a reboot (no root).
 *
 * Upstream did this inside the boot receiver with a 3 second window, which
 * usually ends before Wi-Fi is connected, so it rarely worked. This short
 * foreground service waits for Wi-Fi (adbd only enables wireless debugging on
 * a network the user allowed before), turns wireless debugging on, finds the
 * TLS port over mDNS and runs the starter, retrying a few times. A short
 * service may run for about 3 minutes, which is plenty.
 *
 * Needs Android 13+ (https://r.android.com/2128832) and WRITE_SECURE_SETTINGS,
 * which the server grants to the manager the first time it starts.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class BootStartService : Service() {

    companion object {

        private const val TAG = "BootStartService"
        private const val CHANNEL = "boot_start"
        private const val NOTIFICATION_ID = 3
        private const val RESULT_NOTIFICATION_ID = 4

        private const val WIFI_TIMEOUT_MS = 120_000L
        private const val DISCOVERY_TIMEOUT_MS = 20_000L
        private const val BINDER_TIMEOUT_MS = 10_000L
        private const val ATTEMPTS = 3

        fun canStart(context: Context): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
        }

        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, BootStartService::class.java))
            } catch (e: Throwable) {
                Log.w(TAG, "can't start BootStartService", e)
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private val finished = AtomicBoolean(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, progressNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
            } else {
                startForeground(NOTIFICATION_ID, progressNotification())
            }
        } catch (e: Throwable) {
            Log.w(TAG, "startForeground failed", e)
            stopSelf()
            return START_NOT_STICKY
        }

        if (job?.isActive != true) {
            job = scope.launch {
                val started = try {
                    startService()
                } catch (e: Throwable) {
                    Log.w(TAG, "start on boot failed", e)
                    false
                }
                finish(started)
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun startService(): Boolean {
        if (Shizuku.pingBinder()) return true

        if (!waitForWifi()) {
            Log.w(TAG, "no Wi-Fi within ${WIFI_TIMEOUT_MS / 1000}s")
            return false
        }

        val cr = contentResolver
        repeat(ATTEMPTS) { attempt ->
            // adbd turns wireless debugging off again on networks the user did
            // not allow, so set it on every attempt
            putSetting { Settings.Global.putInt(cr, "adb_wifi_enabled", 1) }
            putSetting { Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1) }
            putSetting { Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L) }

            val port = discoverPort()
            Log.i(TAG, "attempt ${attempt + 1}: adb tls port $port")
            if (port > 0 && runStarter(port)) {
                return true
            }
            if (Shizuku.pingBinder()) return true
            delay(5_000)
        }
        return false
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
            while (!isOnWifi(cm)) delay(2_000)
            true
        } ?: false
    }

    private fun isOnWifi(cm: ConnectivityManager): Boolean {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
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
            AdbClient("127.0.0.1", port, key).use { client ->
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

    private fun finish(started: Boolean) {
        if (!finished.compareAndSet(false, true)) return
        Log.i(TAG, "finished, started=$started")
        if (!started) {
            getSystemService(NotificationManager::class.java)
                .notify(RESULT_NOTIFICATION_ID, failedNotification())
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // Android 14+: a short service must stop when its time is up
    override fun onTimeout(startId: Int) {
        Log.w(TAG, "short service timeout")
        job?.cancel()
        finish(Shizuku.pingBinder())
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

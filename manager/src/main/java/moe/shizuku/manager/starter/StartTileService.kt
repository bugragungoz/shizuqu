package moe.shizuku.manager.starter

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import moe.shizuku.manager.MainActivity
import moe.shizuku.manager.R
import rikka.shizuku.Shizuku

/**
 * Quick Settings tile: shows whether the service runs, and starts it with one tap.
 *
 * The tap does what the app's "Start" button for wireless debugging does, through
 * [BootStartService] (turn wireless debugging on, find its port, run the starter), so it
 * needs Android 13+ and a first start from the app (which grants WRITE_SECURE_SETTINGS).
 * Otherwise, and when the service already runs, the tap opens the app.
 *
 * On the lock screen the phone has to be unlocked first: starting a privileged service
 * must not be possible without unlocking.
 */
class StartTileService : TileService() {

    // when the last start was requested; a start takes at most about 3 minutes
    private var startRequestedAt = 0L
    private val starting
        get() = startRequestedAt != 0L && SystemClock.elapsedRealtime() - startRequestedAt < 180_000

    private val binderReceived = Shizuku.OnBinderReceivedListener {
        startRequestedAt = 0L
        updateTile()
    }
    private val binderDead = Shizuku.OnBinderDeadListener { updateTile() }

    override fun onStartListening() {
        super.onStartListening()
        // the server sends the binder to this process shortly after it starts
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        updateTile()
    }

    override fun onStopListening() {
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun { onClickUnlocked() } else onClickUnlocked()
    }

    private fun onClickUnlocked() {
        if (!Shizuku.pingBinder() && !starting && BootStart.canStart(this) && BootStart.start(this)) {
            startRequestedAt = SystemClock.elapsedRealtime()
            updateTile()
        } else {
            openApp()
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val running = Shizuku.pingBinder()
        tile.label = getString(R.string.app_name)
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(
                when {
                    running -> R.string.tile_running
                    starting -> R.string.tile_starting
                    BootStart.canStart(this) -> R.string.tile_tap_to_start
                    else -> R.string.tile_open_app
                }
            )
        }
        tile.updateTile()
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } catch (e: Throwable) {
            // e.g. the tile is no longer bound by the system UI
            Log.w(TAG, "can't open the app", e)
        }
    }

    private companion object {
        const val TAG = "StartTileService"
    }
}

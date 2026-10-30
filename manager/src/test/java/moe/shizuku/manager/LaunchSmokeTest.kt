package moe.shizuku.manager

import android.app.Activity
import android.util.TypedValue
import moe.shizuku.manager.adb.AdbPairingTutorialActivity
import moe.shizuku.manager.app.Palette
import moe.shizuku.manager.app.ThemeHelper
import moe.shizuku.manager.management.ApplicationManagementActivity
import moe.shizuku.manager.settings.SettingsActivity
import moe.shizuku.manager.shell.ShellTutorialActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Opens the app's screens as real activities (Robolectric, no device) in every palette,
 * light and dark, so a theme or layout mistake that crashes at launch fails the build.
 *
 * ./gradlew :manager:testDebugUnitTest
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = SmokeTestApplication::class)
class LaunchSmokeTest {

    private fun <T : Activity> open(activity: Class<T>, palette: Palette, night: Boolean, check: (T) -> Unit = {}) {
        ThemeHelper.setPalette(palette)
        RuntimeEnvironment.setQualifiers(if (night) "+night" else "+notnight")
        val controller = Robolectric.buildActivity(activity).setup()
        try {
            assertNotNull("${activity.simpleName} has no content", controller.get().window.decorView)
            check(controller.get())
        } finally {
            controller.pause().stop().destroy()
        }
    }

    private fun Activity.colorPrimary(): Int {
        val value = TypedValue()
        theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, value, true)
        return if (value.resourceId != 0) getColor(value.resourceId) else value.data
    }

    @Test
    fun homeOpensInEveryPalette() {
        for (palette in Palette.available()) {
            for (night in listOf(false, true)) {
                open(MainActivity::class.java, palette, night) { activity ->
                    if (palette == Palette.BUGRA) {
                        // the palette overlay really is applied
                        assertEquals(
                            "Bugra primary (night=$night)",
                            if (night) 0xFF3DDC84.toInt() else 0xFF086B37.toInt(),
                            activity.colorPrimary()
                        )
                    }
                }
            }
        }
    }

    @Test
    fun otherScreensOpen() {
        val screens = listOf(
            SettingsActivity::class.java,
            ApplicationManagementActivity::class.java,
            AdbPairingTutorialActivity::class.java,
            ShellTutorialActivity::class.java,
        )
        for (screen in screens) {
            for (night in listOf(false, true)) {
                open(screen, Palette.BUGRA, night)
            }
        }
    }
}

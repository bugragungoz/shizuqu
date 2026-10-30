package moe.shizuku.manager

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import rikka.material.app.LocaleDelegate

/**
 * ShizukuApplication without its static init (it loads the native adb library and
 * calls HiddenApiBypass, neither of which exists on the JVM).
 */
class SmokeTestApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ShizukuSettings.initialize(this)
        LocaleDelegate.defaultLocale = ShizukuSettings.getLocale()
        AppCompatDelegate.setDefaultNightMode(ShizukuSettings.getNightMode())
    }
}

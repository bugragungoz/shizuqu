package moe.shizuku.manager

import android.app.Application

/**
 * ShizukuApplication without its static init (it loads the native adb library and
 * calls HiddenApiBypass, neither of which exists on the JVM).
 */
class SmokeTestApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initApplication(this)
    }
}

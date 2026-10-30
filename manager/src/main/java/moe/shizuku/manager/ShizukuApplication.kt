package moe.shizuku.manager

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.topjohnwu.superuser.Shell
import moe.shizuku.manager.ktx.logd
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.core.util.BuildUtils.atLeast30
import rikka.material.app.LocaleDelegate

lateinit var application: Application

/** Settings, locale and night mode; also used by the smoke tests' application. */
fun initApplication(app: Application) {
    application = app
    ShizukuSettings.initialize(app)
    LocaleDelegate.defaultLocale = ShizukuSettings.getLocale()
    AppCompatDelegate.setDefaultNightMode(ShizukuSettings.getNightMode())
}

class ShizukuApplication : Application() {

    companion object {

        init {
            logd("ShizukuApplication", "init")

            Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_REDIRECT_STDERR))
            if (Build.VERSION.SDK_INT >= 28) {
                // Never let a bypass failure on a new Android version crash the app at launch;
                // features that need hidden APIs fail on their own and are handled there.
                try {
                    if (!HiddenApiBypass.setHiddenApiExemptions("")) {
                        Log.w("ShizukuApplication", "setHiddenApiExemptions returned false")
                    }
                } catch (e: Throwable) {
                    Log.e("ShizukuApplication", "setHiddenApiExemptions failed", e)
                }
            }
            if (atLeast30) {
                System.loadLibrary("adb")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        initApplication(this)
    }

}

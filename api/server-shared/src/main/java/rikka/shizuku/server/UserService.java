package rikka.shizuku.server;

import android.app.ActivityThread;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextHidden;
import android.ddm.DdmHandleAppName;
import android.os.Build;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.UserHandleHidden;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dev.rikka.tools.refine.Refine;

public class UserService {

    private static String TAG;

    public static void setTag(String tag) {
        UserService.TAG = tag;
    }

    @Nullable
    public static Pair<IBinder, String> create(String[] args) {
        String name = null;
        String token = null;
        String pkg = null;
        String cls = null;
        int uid = -1;

        for (String arg : args) {
            if (arg.startsWith("--debug-name=")) {
                name = arg.substring(13);
            } else if (arg.startsWith("--token=")) {
                token = arg.substring(8);
            } else if (arg.startsWith("--package=")) {
                pkg = arg.substring(10);
            } else if (arg.startsWith("--class=")) {
                cls = arg.substring(8);
            } else if (arg.startsWith("--uid=")) {
                uid = Integer.parseInt(arg.substring(6));
            }
        }

        int userId = uid / 100000;

        Log.i(TAG, String.format("starting service %s/%s...", pkg, cls));

        IBinder service;

        try {
            ActivityThread activityThread = ActivityThread.systemMain();
            Context systemContext = activityThread.getSystemContext();

            DdmHandleAppName.setAppName(name != null ? name : pkg + ":user_service", userId);

            //noinspection InstantiationOfUtilityClass
            UserHandle userHandle = Refine.unsafeCast(
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                            ? UserHandleHidden.of(userId)
                            : new UserHandleHidden(userId));
            Context context = Refine.<ContextHidden>unsafeCast(systemContext).createPackageContextAsUser(pkg, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY, userHandle);
            Field mPackageInfo = context.getClass().getDeclaredField("mPackageInfo");
            mPackageInfo.setAccessible(true);
            Object loadedApk = mPackageInfo.get(context);
            Method makeApplication = loadedApk.getClass().getDeclaredMethod("makeApplication", boolean.class, Instrumentation.class);
            Application application;
            try {
                application = (Application) makeApplication.invoke(loadedApk, true, null);
            } catch (InvocationTargetException e) {
                // Some OEM frameworks (HyperOS, Transsion, possibly others) read the bound
                // application's process name / package name inside makeApplication. This
                // process was started by ActivityThread.systemMain(), so mBoundApplication is
                // null and they throw a NullPointerException. Give them a minimal AppBindData
                // and try once more. Stock Android never gets here.
                // Ref: https://github.com/RikkaApps/Shizuku/issues/1198
                if (!causedBy(e, NullPointerException.class)
                        || !installBoundApplication(activityThread, loadedApk, name != null ? name : pkg + ":user_service")) {
                    throw e;
                }
                Log.w(TAG, "makeApplication failed with a NullPointerException, retrying with a bound application", e.getCause());
                application = (Application) makeApplication.invoke(loadedApk, true, null);
            }
            Field mInitialApplication = activityThread.getClass().getDeclaredField("mInitialApplication");
            mInitialApplication.setAccessible(true);
            mInitialApplication.set(activityThread, application);

            ClassLoader classLoader = application.getClassLoader();
            Class<?> serviceClass = classLoader.loadClass(cls);
            Constructor<?> constructorWithContext = null;
            try {
                constructorWithContext = serviceClass.getConstructor(Context.class);
            } catch (NoSuchMethodException | SecurityException ignored) {
            }
            if (constructorWithContext != null) {
                service = (IBinder) constructorWithContext.newInstance(application);
            } else {
                service = (IBinder) serviceClass.newInstance();
            }
        } catch (Throwable tr) {
            Log.w(TAG, String.format("unable to start service %s/%s...", pkg, cls), tr);
            return null;
        }

        return new Pair<>(service, token);
    }

    private static boolean installBoundApplication(ActivityThread activityThread, Object loadedApk, String processName) {
        try {
            Field mBoundApplication = activityThread.getClass().getDeclaredField("mBoundApplication");
            mBoundApplication.setAccessible(true);
            if (mBoundApplication.get(activityThread) != null) {
                return false;
            }

            // ActivityThread.currentPackageName() dereferences appInfo once
            // mBoundApplication is set, so never install one without it.
            Object appInfo = loadedApk.getClass().getMethod("getApplicationInfo").invoke(loadedApk);
            if (appInfo == null) {
                return false;
            }

            Class<?> appBindDataClass = Class.forName("android.app.ActivityThread$AppBindData");
            Constructor<?> constructor = appBindDataClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object appBindData = constructor.newInstance();
            setField(appBindDataClass, appBindData, "processName", processName);
            setField(appBindDataClass, appBindData, "appInfo", appInfo);
            setField(appBindDataClass, appBindData, "info", loadedApk);
            try {
                Object compatInfo = Class.forName("android.content.res.CompatibilityInfo")
                        .getField("DEFAULT_COMPATIBILITY_INFO").get(null);
                setField(appBindDataClass, appBindData, "compatInfo", compatInfo);
            } catch (Throwable ignored) {
            }

            mBoundApplication.set(activityThread, appBindData);
            return true;
        } catch (Throwable tr) {
            Log.w(TAG, "installBoundApplication", tr);
            return false;
        }
    }

    private static boolean causedBy(Throwable t, Class<? extends Throwable> type) {
        for (int depth = 0; t != null && depth < 10; t = t.getCause(), depth++) {
            if (type.isInstance(t)) return true;
        }
        return false;
    }

    private static void setField(Class<?> cls, Object obj, String name, Object value) {
        try {
            Field field = cls.getDeclaredField(name);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Throwable ignored) {
            // not present on this Android version
        }
    }
}

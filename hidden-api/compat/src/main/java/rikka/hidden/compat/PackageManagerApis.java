package rikka.hidden.compat;

import static rikka.hidden.compat.Services.packageManager;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.IPackageManager37;
import android.content.pm.PackageInfo;
import android.content.pm.ParceledListSlice;
import android.content.pm.ProviderInfo;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.rikka.tools.refine.Refine;
import rikka.buildcompat.BuildCompat;
import rikka.hidden.compat.util.ReflectCompat;

@SuppressWarnings("unused")
public class PackageManagerApis {

    private static final String TAG = "PackageManagerApis";

    @SuppressLint("NewApi")
    @Nullable
    public static PackageInfo getPackageInfo(@Nullable String packageName, long flags, int userId) throws RemoteException {
        if (BuildCompat.isAtLeastT()) {
            return packageManager.get().getPackageInfo(packageName, flags, userId);
        } else {
            return packageManager.get().getPackageInfo(packageName, (int) flags, userId);
        }
    }

    @SuppressLint("NewApi")
    @Nullable
    public static ApplicationInfo getApplicationInfo(@Nullable String packageName, long flags, int userId) throws RemoteException {
        if (BuildCompat.isAtLeastT()) {
            return packageManager.get().getApplicationInfo(packageName, flags, userId);
        } else {
            return packageManager.get().getApplicationInfo(packageName, (int) flags, userId);
        }
    }

    @Nullable
    public static PackageInfo getPackageInfoNoThrow(@Nullable String packageName, long flags, int userId) {
        try {
            return getPackageInfo(packageName, flags, userId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static ApplicationInfo getApplicationInfoNoThrow(@Nullable String packageName, long flags, int userId) {
        try {
            return getApplicationInfo(packageName, flags, userId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static String[] getPackagesForUid(int uid) throws RemoteException {
        return packageManager.get().getPackagesForUid(uid);
    }

    @SuppressLint("NewApi")
    @Nullable
    public static ParceledListSlice<ApplicationInfo> getInstalledApplications(long flags, int userId) throws RemoteException {
        IPackageManager pm = packageManager.get();
        if (!BuildCompat.isAtLeastT()) {
            //noinspection unchecked
            return pm.getInstalledApplications((int) flags, userId);
        }
        try {
            //noinspection unchecked
            return pm.getInstalledApplications(flags, userId);
        } catch (NoSuchMethodError e) {
            // Not changed as of Android 17 QPR1, but getInstalledPackages was, so be ready
            //noinspection unchecked
            return (ParceledListSlice<ApplicationInfo>) ReflectCompat.invokeAnyReturnType(pm,
                    "getInstalledApplications", new Class<?>[]{long.class, int.class}, flags, userId);
        }
    }

    /**
     * Returns installed packages.
     * <p>
     * Android 17 (API 37) changed the return type of
     * {@code IPackageManager#getInstalledPackages(long, int)} from {@code ParceledListSlice}
     * to {@code PackageInfoList} (a subclass of {@code ParceledListSlice}). Calling the old
     * descriptor throws {@link NoSuchMethodError} there, so both variants are tried, and a
     * reflection fallback covers future return type changes.
     */
    @SuppressLint("NewApi")
    @Nullable
    public static ParceledListSlice<PackageInfo> getInstalledPackages(long flags, int userId) throws RemoteException {
        IPackageManager pm = packageManager.get();
        if (!BuildCompat.isAtLeastT()) {
            //noinspection unchecked
            return pm.getInstalledPackages((int) flags, userId);
        }

        boolean preferApi37 = Build.VERSION.SDK_INT >= 37;
        if (preferApi37) {
            try {
                return Refine.<IPackageManager37>unsafeCast(pm).getInstalledPackages(flags, userId);
            } catch (NoSuchMethodError ignored) {
            }
        }
        try {
            //noinspection unchecked
            return pm.getInstalledPackages(flags, userId);
        } catch (NoSuchMethodError e) {
            if (!preferApi37) {
                // Android 17 preview builds still report SDK_INT 36
                try {
                    return Refine.<IPackageManager37>unsafeCast(pm).getInstalledPackages(flags, userId);
                } catch (NoSuchMethodError ignored) {
                }
            }
            //noinspection unchecked
            return (ParceledListSlice<PackageInfo>) ReflectCompat.invokeAnyReturnType(pm,
                    "getInstalledPackages", new Class<?>[]{long.class, int.class}, flags, userId);
        }
    }


    @NonNull
    public static List<PackageInfo> getInstalledPackagesNoThrow(long flags, int userId) {
        try {
            ParceledListSlice<PackageInfo> parceledListSlice = getInstalledPackages(flags, userId);
            if (parceledListSlice != null && parceledListSlice.getList() != null) {
                return parceledListSlice.getList();
            }
        } catch (Throwable e) {
            Log.w(TAG, "getInstalledPackages failed (flags=" + flags + ", userId=" + userId + ")", e);
        }
        return Collections.emptyList();
    }

    @NonNull
    public static List<ApplicationInfo> getInstalledApplicationsNoThrow(long flags, int userId) {
        try {
            ParceledListSlice<ApplicationInfo> parceledListSlice = getInstalledApplications(flags, userId);
            if (parceledListSlice != null && parceledListSlice.getList() != null) {
                return parceledListSlice.getList();
            }
        } catch (Throwable e) {
            Log.w(TAG, "getInstalledApplications failed (flags=" + flags + ", userId=" + userId + ")", e);
        }
        return Collections.emptyList();
    }

    @NonNull
    public static List<String> getPackagesForUidNoThrow(int uid) {
        ArrayList<String> packages = new ArrayList<>();

        try {
            String[] packagesArray = getPackagesForUid(uid);
            if (packagesArray != null) {
                for (String packageName : packagesArray) {
                    if (packageName != null) {
                        packages.add(packageName);
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return packages;
    }

    @SuppressLint("NewApi")
    public static int getPackageUid(String packageName, long flags, int userId) throws RemoteException {
        IPackageManager pm = packageManager.get();
        if (BuildCompat.isAtLeastT()) {
            return pm.getPackageUid(packageName, flags, userId);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return pm.getPackageUid(packageName, (int) flags, userId);
        } else {
            return pm.getPackageUid(packageName, userId);
        }
    }


    public static int getUidForSharedUser(String sharedUserName) throws RemoteException {
        return packageManager.get().getUidForSharedUser(sharedUserName);
    }

    public static int getUidForSharedUserNoThrow(String sharedUserName) {
        try {
            return getUidForSharedUser(sharedUserName);
        } catch (Throwable tr) {
            return -1;
        }
    }

    @SuppressLint("NewApi")
    @Nullable
    public static ProviderInfo resolveContentProvider(String name, long flags, int userId) throws RemoteException {
        if (BuildCompat.isAtLeastT()) {
            return packageManager.get().resolveContentProvider(name, flags, userId);
        } else {
            return packageManager.get().resolveContentProvider(name, (int) flags, userId);
        }
    }

    public static boolean getApplicationHiddenSettingAsUser(String packageName, int userId) throws RemoteException {
        return packageManager.get().getApplicationHiddenSettingAsUser(packageName, userId);
    }

    public static boolean getApplicationHiddenSettingAsUserNoThrow(String packageName, int userId) {
        try {
            return getApplicationHiddenSettingAsUser(packageName, userId);
        } catch (Throwable tr) {
            return true;
        }
    }

    public static int checkSignatures(String pkg1, String pkg2, int userId) throws RemoteException {
        if (BuildCompat.isAtLeastT()) {
            try {
                return packageManager.get().checkSignatures(pkg1, pkg2, userId);
            } catch (NoSuchMethodError ignored) {
            }
        }
        return packageManager.get().checkSignatures(pkg1, pkg2);
    }

    public int checkSignatures(String pkg1, String pkg2) throws RemoteException {
        return checkSignatures(pkg1, pkg2, 0);
    }

    public int checkUidSignatures(int uid1, int uid2) throws RemoteException {
        return packageManager.get().checkUidSignatures(uid1, uid2);
    }
}

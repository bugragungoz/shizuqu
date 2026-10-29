package android.content.pm;

import android.os.IInterface;
import android.os.RemoteException;

import dev.rikka.tools.refine.RefineAs;

/**
 * Methods of {@link IPackageManager} whose signature changed in Android 17 (API 37).
 * <p>
 * A Java interface cannot declare two methods that only differ in return type, so the
 * Android 17 variants live here and are renamed to {@code IPackageManager} by Refine.
 */
@RefineAs(IPackageManager.class)
public interface IPackageManager37 extends IInterface {

    // Android 17: return type changed from ParceledListSlice to PackageInfoList
    PackageInfoList getInstalledPackages(long flags, int userId)
            throws RemoteException;
}

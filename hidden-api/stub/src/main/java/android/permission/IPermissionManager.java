package android.permission;

import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

@RequiresApi(30)
public interface IPermissionManager extends IInterface {

    void grantRuntimePermission(String packageName, String permissionName, int userId)
            throws RemoteException;

    void grantRuntimePermission(String packageName, String permissionName, int deviceId, int userId)
            throws RemoteException;

     void grantRuntimePermission(String packageName, String permissionName, String persistentDeviceId, int userId)
            throws RemoteException;

    void revokeRuntimePermission(String packageName, String permissionName, int userId)
            throws RemoteException;

    void revokeRuntimePermission(String packageName, String permissionName, int userId, String reason)
            throws RemoteException;

    void revokeRuntimePermission(String packageName, String permissionName, int deviceId, int userId, String reason)
            throws RemoteException;

    void revokeRuntimePermission(String packageName, String permissionName, String persistentDeviceId, int userId, String reason)
            throws RemoteException;

    // Android 14 QPR+ (persistentDeviceId), still the signature on Android 17
    int getPermissionFlags(String packageName, String permissionName, String persistentDeviceId, int userId)
            throws RemoteException;

    // Android 14 QPR2 (int deviceId)
    int getPermissionFlags(String packageName, String permissionName, int deviceId, int userId)
            throws RemoteException;

    int getPermissionFlags(String permissionName, String packageName, int userId)
            throws RemoteException;

    // Android 14 QPR+ (persistentDeviceId), still the signature on Android 17
    void updatePermissionFlags(String packageName, String permissionName, int flagMask, int flagValues, boolean checkAdjustPolicyFlagPermission, String persistentDeviceId, int userId)
            throws RemoteException;

    // Android 14 QPR2 (int deviceId)
    void updatePermissionFlags(String packageName, String permissionName, int flagMask, int flagValues, boolean checkAdjustPolicyFlagPermission, int deviceId, int userId)
            throws RemoteException;

    void updatePermissionFlags(String permissionName, String packageName, int flagMask, int flagValues, boolean checkAdjustPolicyFlagPermission, int userId)
            throws RemoteException;

    int checkPermission(String permName, String pkgName, int userId)
            throws RemoteException;

    int checkUidPermission(String permName, int uid)
            throws RemoteException;

    PermissionGroupInfo getPermissionGroupInfo(String groupName, int flags)
            throws RemoteException;

    PermissionInfo getPermissionInfo(String permissionName, String packageName, int flags)
            throws RemoteException;

    @RequiresApi(30)
    abstract class Stub extends Binder implements IPermissionManager {

        public static IPermissionManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}

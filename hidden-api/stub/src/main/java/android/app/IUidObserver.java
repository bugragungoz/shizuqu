package android.app;

import android.os.Binder;
import android.os.RemoteException;

public interface IUidObserver {

    void onUidGone(int uid) throws RemoteException;

    void onUidGone(int uid, boolean disabled) throws RemoteException;

    void onUidActive(int uid) throws RemoteException;

    void onUidIdle(int uid) throws RemoteException;

    void onUidIdle(int uid, boolean disabled) throws RemoteException;

    void onUidStateChanged(int uid, int procState) throws RemoteException;

    void onUidStateChanged(int uid, int procState, long procStateSeq) throws RemoteException;

    void onUidStateChanged(int uid, int procState, long procStateSeq, int capability) throws RemoteException;

    void onUidCachedChanged(int uid, boolean cached) throws RemoteException;

    // Android 14: uid only; later versions (17 included) also pass the adj
    void onUidProcAdjChanged(int uid) throws RemoteException;

    void onUidProcAdjChanged(int uid, int adj) throws RemoteException;

    abstract class Stub extends Binder implements IUidObserver {

    }
}

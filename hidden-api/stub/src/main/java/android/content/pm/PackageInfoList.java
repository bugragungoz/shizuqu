package android.content.pm;

import java.util.List;

/**
 * Added in Android 17 (API 37).
 * <p>
 * {@code IPackageManager#getInstalledPackages(long, int)} returns this type instead of
 * {@link ParceledListSlice} since Android 17. It is a subclass of {@link ParceledListSlice},
 * so {@code getList()} keeps working.
 */
public final class PackageInfoList extends ParceledListSlice<PackageInfo> {

    public PackageInfoList(List<PackageInfo> list) {
        super(list);
        throw new RuntimeException("STUB");
    }
}

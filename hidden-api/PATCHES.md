# Changes to the vendored HiddenApi

Base: [RikkaW/HiddenApi](https://github.com/RikkaW/HiddenApi) @ `28122e6`
(= `dev.rikka.hidden:stub` / `compat` 4.4.0), MIT License.

| File | Change |
|---|---|
| `stub/.../content/pm/PackageInfoList.java` | new: Android 17 return type of `getInstalledPackages` |
| `stub/.../content/pm/IPackageManager37.java` | new: Android 17 signatures, `@RefineAs(IPackageManager.class)` |
| `stub/.../content/pm/IPackageManager.java` | `checkSignatures(String, String, int)` (Android 13+) |
| `stub/.../permission/IPermissionManager.java` | `get/updatePermissionFlags` with `persistentDeviceId` (Android 14 QPR+) |
| `stub/.../app/IUidObserver.java` | `onUidProcAdjChanged(int, int)` (Android 14+) |
| `compat/.../PackageManagerApis.java` | `getInstalledPackages`: Android 17 signature first, old one, then reflection; `getInstalledApplications`: reflection fallback; log failures; static `checkSignatures(String, String, int)` |
| `compat/.../UserManagerApis.java` | `getUsers`: `getUsers(boolean)` on Android 17 (3-argument version removed) |
| `compat/.../PermissionManagerApis.java` | try the `persistentDeviceId` variants of `get/updatePermissionFlags` first |
| `compat/.../adapter/UidObserverAdapter.java` | implement `onUidProcAdjChanged` |
| `compat/.../util/ReflectCompat.java` | new: call a method by name + parameter types, ignoring the return type |
| `*/build.gradle` | no Maven publishing; module names `:hidden-stub` / `:hidden-compat` |

See [docs/ANDROID17.md](../docs/ANDROID17.md) for why.

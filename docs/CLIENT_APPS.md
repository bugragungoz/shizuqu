# Apps that use Shizuku on Android 17

Shizuqu speaks the unchanged Shizuku protocol, so apps built with
`dev.rikka.shizuku:api` 13.1.5 work with it as they did with Shizuku.

When an app breaks on Android 17, the cause is usually its own hidden API
calls made through `ShizukuBinderWrapper`. Those calls run the phone's
`IXxx.Stub.Proxy` code. If your stub declares an old signature, ART throws
`NoSuchMethodError`, and a `catch (Throwable)` block turns that into an empty
result.

## Hidden APIs that changed

**`IPackageManager.getInstalledPackages(long, int)` returns `PackageInfoList`**
(a subclass of `ParceledListSlice`). There are three ways to deal with this:

* Use the public `PackageManager` for your own user.
* Call the method by reflection, which ignores the return type:

  ```kotlin
  val m = pm.javaClass.getMethod("getInstalledPackages", Long::class.javaPrimitiveType, Int::class.javaPrimitiveType)
  val slice = m.invoke(pm, flags, userId)
  val list = slice.javaClass.getMethod("getList").invoke(slice) as List<PackageInfo>
  ```

* Use `hidden-api/` from this repository. Its `PackageManagerApis` already
  tries both signatures.

**`IUserManager.getUsers(boolean, boolean, boolean)` was removed.** Call
`getUsers(boolean excludeDying)` on API 37. If you fall back to "user 0 only",
apps in the work profile, Secure Folder (user 150) and Dual Messenger
(user 95) disappear.

```kotlin
val users = try {
    if (Build.VERSION.SDK_INT >= 37) um.getUsers(true) else um.getUsers(true, true, true)
} catch (e: NoSuchMethodError) { um.getUsers(true, true, true) }
```

**The permission APIs take a device id.** For example:

```java
grantRuntimePermission(String pkg, String perm, String persistentDeviceId, int userId)
```

For the phone itself, pass `"default:0"` as `persistentDeviceId` and `0` as
`deviceId`.

**In 17.1:** `getDelegatedShellPermissions`,
`stopDelegateShellPermissionIdentity` and `killApplication` in
`IActivityManager` gained an argument.

To check your own APK against the Android 17 framework:

```bash
python3 tools/hiddenapi-check/check.py check app.apk \
    tools/hiddenapi-check/frameworks/android-37.0.sigs.gz --only android/,com/android/
```

## Platform changes that matter for these apps

| Change | What to do |
|---|---|
| `ADB_ENABLED` and `DEVELOPMENT_SETTINGS_ENABLED` read as `0` for normal apps | Treat `0` as unknown. Read the real value with shell rights: `settings get global adb_enabled` |
| Apps that target 37 need `ACCESS_LOCAL_NETWORK` for LAN sockets, mDNS and `NsdManager` | Request it, or use the `NsdManager` system picker |
| Apps that target 37 can no longer change `static final` fields by reflection, and `System.load()` needs read-only files | Stop relying on either |

## Making the next update easier

* Prefer **user services** (`Shizuku.bindUserService`). Your code then runs
  with shell or root rights, and can use `pm`, `cmd`, `settings` and
  `dumpsys` instead of hidden binder methods.
* Keep all hidden calls in one wrapper class. Try the newest signature first,
  fall back on `NoSuchMethodError`, and log the failure instead of returning
  an empty result.
* Don't depend on the manager's package name. To find it, look for the app
  that declares `moe.shizuku.manager.permission.API_V23`:

  ```kotlin
  pm.getPermissionInfo("moe.shizuku.manager.permission.API_V23", 0).packageName
  ```

* When a new Android version comes out, run the checker above against its
  `framework.jar`. [DEVELOPMENT.md](DEVELOPMENT.md#a-new-android-version)
  shows how.

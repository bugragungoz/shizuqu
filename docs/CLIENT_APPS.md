# Writing (and updating) apps that use Shizuku on Android 17+

This fork speaks the unchanged Shizuku protocol. An app built with the
official client library (`dev.rikka.shizuku:api` / `provider`, 13.1.5) works
with it exactly like with Shizuku. What breaks on Android 17 is usually **the
app's own hidden API calls** made through `ShizukuBinderWrapper` - the same
kind of breakage this fork had to fix in the server (see
[ANDROID17.md](ANDROID17.md)). This page lists what to do in your app.

## 1. Setup (unchanged)

```groovy
dependencies {
    def shizuku_version = '13.1.5'
    implementation "dev.rikka.shizuku:api:$shizuku_version"
    implementation "dev.rikka.shizuku:provider:$shizuku_version"
}
```

```xml
<provider
    android:name="rikka.shizuku.ShizukuProvider"
    android:authorities="${applicationId}.shizuku"
    android:enabled="true"
    android:exported="true"
    android:multiprocess="false"
    android:permission="android.permission.INTERACT_ACROSS_USERS_FULL" />
```

The provider's manifest adds `moe.shizuku.manager.permission.API_V23` and the
`moe.shizuku.client.V3_SUPPORT` meta-data. The server finds apps by these two,
so do not remove them.

```kotlin
Shizuku.addBinderReceivedListenerSticky { /* binder is alive */ }
Shizuku.addBinderDeadListener { /* service stopped */ }
Shizuku.addRequestPermissionResultListener { requestCode, result -> /* ... */ }

fun ensurePermission(code: Int): Boolean = when {
    !Shizuku.pingBinder() -> false
    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> true
    Shizuku.shouldShowRequestPermissionRationale() -> false // denied forever
    else -> { Shizuku.requestPermission(code); false }
}
```

`Shizuku.getUid()` is `2000` (ADB/shell) or `0` (root). Shell's permissions
are listed in AOSP's `frameworks/base/packages/Shell/AndroidManifest.xml`;
Android 17 removed none and added 45 compared to 16 QPR2.

### Don't depend on the manager's package name

The manager's package name is configurable in this fork (`branding.properties`).
Use `Shizuku.pingBinder()` / the binder listeners to know whether the service
runs. If you want to open the manager or check that it is installed, accept
both `moe.shizuku.privileged.api` and the fork's id, or find the app that
declares the `moe.shizuku.manager.permission.API_V23` permission:

```kotlin
fun findShizukuManager(pm: PackageManager): String? = try {
    pm.getPermissionInfo("moe.shizuku.manager.permission.API_V23", 0).packageName
} catch (e: PackageManager.NameNotFoundException) { null }
```

## 2. Hidden API calls that changed in Android 17

Calls through `ShizukuBinderWrapper` run the **device's** `IXxx.Stub.Proxy`
code inside your app. If your stub (for example `dev.rikka.hidden:stub` 4.4.0)
declares an old signature, ART throws `NoSuchMethodError` at the call site.
Catch blocks that swallow `Throwable` turn this into "empty list" bugs.

### 2.1 `IPackageManager.getInstalledPackages(long, int)` returns `PackageInfoList`

```java
// Android 16 stub:  ParceledListSlice<PackageInfo> getInstalledPackages(long flags, int userId)
// Android 17:       PackageInfoList                getInstalledPackages(long flags, int userId)
```

Options, best first:

1. **Avoid the hidden call.** For your own user, the public
   `context.packageManager.getInstalledPackages(PackageInfoFlags.of(flags))`
   (with `QUERY_ALL_PACKAGES` or `<queries>`) is enough. For another user,
   run `pm list packages --user <id>` in a [user service](#4-prefer-user-services)
   or use the method below.
2. **Call it by reflection** (ignores the return type, works on every version):

   ```kotlin
   @Suppress("UNCHECKED_CAST")
   fun installedPackages(pm: IPackageManager, flags: Long, userId: Int): List<PackageInfo> {
       val m = pm.javaClass.getMethod("getInstalledPackages", Long::class.javaPrimitiveType, Int::class.javaPrimitiveType)
       val slice = m.invoke(pm, flags, userId) ?: return emptyList()
       // ParceledListSlice and PackageInfoList both have getList()
       return slice.javaClass.getMethod("getList").invoke(slice) as List<PackageInfo>
   }
   val pm = IPackageManager.Stub.asInterface(ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package")))
   ```

   `HiddenApiBypass` (6.2+) or another bypass must be active for reflection on
   `@hide` members in a normal app.
3. **Use the patched stub/compat from this repo** (`hidden-api/`): the stub has
   `android.content.pm.IPackageManager37` (renamed to `IPackageManager` by the
   [Refine](https://github.com/RikkaApps/HiddenApiRefinePlugin) plugin) and
   `PackageManagerApis.getInstalledPackages()` already tries both signatures.

### 2.2 `IUserManager.getUsers(boolean, boolean, boolean)` is gone

```java
// Android 11-16: getUsers(boolean excludePartial, boolean excludeDying, boolean excludePreCreated)
// Android 17:    getUsers(boolean excludeDying)   // partial + pre-created always excluded
```

```kotlin
fun users(um: IUserManager): List<UserInfo> = try {
    if (Build.VERSION.SDK_INT >= 37) um.getUsers(true) else um.getUsers(true, true, true)
} catch (e: NoSuchMethodError) {
    um.getUsers(true, true, true)
}
```

If you only catch and fall back to "user 0", work profiles, Samsung Secure
Folder (user 150) and Dual Messenger (user 95) disappear.

### 2.3 Permission APIs take a device id

Since Android 14 QPR the permission manager is per virtual device. On
Android 17 the signatures are:

```java
IPermissionManager.grantRuntimePermission(String pkg, String perm, String persistentDeviceId, int userId);
IPermissionManager.revokeRuntimePermission(String pkg, String perm, String persistentDeviceId, int userId, String reason);
IPermissionManager.getPermissionFlags(String pkg, String perm, String persistentDeviceId, int userId);
IPermissionManager.updatePermissionFlags(String pkg, String perm, int mask, int values, boolean checkAdjustPolicy, String persistentDeviceId, int userId);
IPermissionManager.checkPermission(String pkg, String perm, String persistentDeviceId, int userId);
IPermissionManager.checkUidPermission(int uid, String perm, int deviceId);
IPackageManager.checkUidPermission(String perm, int uid);   // still there, default device
```

Use `"default:0"` (`VirtualDeviceManager.PERSISTENT_DEVICE_ID_DEFAULT`) and
`0` (`Context.DEVICE_ID_DEFAULT`) for the phone itself. Try the newest
signature first and fall back on `NoSuchMethodError`.

### 2.4 Changed in Android 17 QPR1 (17.1)

`IActivityManager.getDelegatedShellPermissions(int)`,
`stopDelegateShellPermissionIdentity(int)`,
`killApplication(String, int, int, String, int, int)` and
`addApplicationStartInfoCompleteListener(listener, String, int)` gained an
argument. Check the [framework dumps](../tools/hiddenapi-check/frameworks) before
using them.

### 2.5 Check your APK automatically

The checker in this repo finds every framework method/field your APK
references that does not exist on a given build:

```bash
python3 tools/hiddenapi-check/check.py check app-release.apk \
    tools/hiddenapi-check/frameworks/android-37.0.sigs.gz --only android/,com/android/
```

Dump your own phone's framework to check against the exact ROM (Samsung adds
methods, rarely changes AOSP ones):

```bash
adb pull /system/framework/framework.jar
python3 tools/hiddenapi-check/check.py dump-framework framework.jar my-phone.sigs
```

## 3. Platform changes in Android 17 that matter for Shizuku apps

| Change | Who is affected | What to do |
|---|---|---|
| `Settings.Global.ADB_ENABLED` and `DEVELOPMENT_SETTINGS_ENABLED` read as `0` for third-party apps (flag-gated, Pixels since Sept 2026) | apps that check "is USB debugging on?" | treat `0` as unknown; `adb_wifi_enabled` is not redacted; read the real value through Shizuku (below) |
| `ACCESS_LOCAL_NETWORK` runtime permission (NEARBY_DEVICES group) | apps **targeting 37** that use LAN sockets, mDNS or `NsdManager` | request it, or use the `NsdManager` system picker (`DiscoveryRequest.FLAG_SHOW_PICKER`) |
| Reflection can't change `static final` fields (`IllegalAccessException`) | apps targeting 37 | stop patching static finals by reflection |
| `MessageQueue` is lock-free; reflection on its private fields breaks | apps targeting 37 | stop reflecting on `MessageQueue` |
| `System.load()` requires read-only files | apps targeting 37 loading native code from writable paths | `chmod` to read-only or load from `nativeLibraryDir` |
| BAL (background activity launch) rules extended to `IntentSender` | apps that start activities via `IntentSender` from background | migrate away from `MODE_BACKGROUND_ACTIVITY_START_ALLOWED` |
| Cross-profile loopback traffic blocked by default | apps talking over 127.0.0.1 between profiles | use binder / a user service instead |

Reading a redacted setting with shell rights (inside a user service, which
runs as shell/root):

```java
// in your UserService (runs in the Shizuku process context)
Process p = Runtime.getRuntime().exec(new String[]{"settings", "get", "global", "adb_enabled"});
String value = new BufferedReader(new InputStreamReader(p.getInputStream())).readLine(); // "1" or "0"
```

## 4. Prefer user services

`Shizuku.bindUserService()` runs your own class in a process with shell/root
identity. You call **public and hidden APIs locally there**, not through
binder proxies of your app, and you can use plain Java (`Runtime.exec`,
files under `/data/local/tmp`, ...). It is the most robust way to survive
signature changes: you can call `pm`, `cmd`, `settings`, `dumpsys` instead of
hidden binder methods.

```kotlin
val args = Shizuku.UserServiceArgs(ComponentName(BuildConfig.APPLICATION_ID, MyService::class.java.name))
    .daemon(false)
    .processNameSuffix("svc")
    .debuggable(BuildConfig.DEBUG)
    .version(BuildConfig.VERSION_CODE)
Shizuku.bindUserService(args, connection)
```

`Shizuku.newProcess()` is private since API 13; use a user service instead.

## 5. Keeping your app ready for the next Android version

1. Keep **every** hidden call in one wrapper class, never sprinkled around.
2. In that wrapper, try the newest signature first, catch `NoSuchMethodError`
   and fall back. Log failures, never return an empty list silently.
3. For return-type changes, declare the new variant in a separate stub
   interface with `@RefineAs(IOriginal.class)` (like `IPackageManager37`).
4. Keep `HiddenApiBypass` (or your bypass) up to date.
5. When a new Android version or beta ships, dump its `framework.jar`
   (emulator image or your phone) and run the checker on your APK - see
   [UPDATING_FOR_NEW_ANDROID.md](UPDATING_FOR_NEW_ANDROID.md).
6. Raise `targetSdk` deliberately and read the "behavior changes: apps
   targeting N" page first; `compileSdk` can move ahead of it.

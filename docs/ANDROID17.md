# Android 17 (API 37): what broke in Shizuku and how this fork fixes it

Upstream Shizuku 13.6.0 (last commit June 2025) was built for Android 16. On
Android 17 the server still starts, but:

* the **"Authorized applications" list is empty** (0 apps), and
* apps that were already running when the service started **never receive the
  binder**, so they act as if Shizuku is not running;
* apps in **other users / profiles** (work profile, Samsung Secure Folder,
  Dual Messenger, ...) never receive the binder at all.

Everything below was verified against the real Android 17 framework, not
guessed:

| Source | Build |
|---|---|
| `framework.jar` of the Android 17.0 emulator image (google_apis x86_64) | `CE2A.260420.051.A3`, patch 2026-08-05 |
| `framework.jar` of the Android 17.1 (QPR1) emulator image | `CP31.260623.012`, patch 2026-07-05 |
| AOSP `android17-release` branch (`frameworks/base`, `packages/modules/adb`) | android-17.0.0_r1 |

The method/field signatures of both framework builds are in
[`tools/hiddenapi-check/frameworks`](../tools/hiddenapi-check/frameworks) and
are checked against every build in CI (see
[UPDATING_FOR_NEW_ANDROID.md](UPDATING_FOR_NEW_ANDROID.md)).

## 1. `IPackageManager.getInstalledPackages()` return type changed

```
Android 16: ParceledListSlice getInstalledPackages(long flags, int userId);
Android 17: PackageInfoList   getInstalledPackages(long flags, in int userId);
```

`PackageInfoList` is a new `@hide` subclass of `ParceledListSlice<PackageInfo>`
that dedups strings when parcelling. Java/ART links a call by name **and
return type**, so code compiled against the old stub throws
`NoSuchMethodError` on Android 17. In upstream Shizuku the call goes through
`PackageManagerApis.getInstalledPackagesNoThrow()` from
`dev.rikka.hidden:compat`, which catches every `Throwable` and returns an
empty list. The result:

| Caller | Effect on Android 17 |
|---|---|
| `ShizukuService.getApplications()` | manager shows 0 apps |
| `ShizukuService.sendBinderToClient()` (on server start) | running apps never get the binder |
| `ShizukuConfigManager` (on server start) | runtime-permission grants are not imported into the config |
| manager, pre-v11 path | empty list |

`getInstalledApplications()` still returns `ParceledListSlice` in 17.0 and 17.1.

**Fix** (`hidden-api/`): `IPackageManager37` declares the new signature and is
renamed to `IPackageManager` at build time by Refine. `PackageManagerApis`
tries the Android 17 signature first, falls back to the old one (and to the
new one on preview builds that still report SDK 36), and as a last resort
calls the method by reflection, which ignores the return type. Because
`PackageInfoList extends ParceledListSlice`, all callers keep working
unchanged. Failures are now logged instead of silently swallowed.

## 2. `IUserManager.getUsers(boolean, boolean, boolean)` was removed

```
Android 16: List<UserInfo> getUsers(boolean excludePartial, boolean excludeDying, boolean excludePreCreated);
Android 17: List<UserInfo> getUsers(boolean excludeDying);   // always excludes partial + pre-created
```

`UserManagerApis.getUserIdsNoThrow()` caught the `NoSuchMethodError` and
returned only user 0. So on Android 17 upstream Shizuku only looks at the
owner user: apps in a work profile, **Samsung Secure Folder (user 150) or Dual
Messenger (user 95)** do not show up and never get the binder.

**Fix**: `UserManagerApis.getUsers()` uses `getUsers(boolean)` on API 37+
(and as a fallback on preview builds).

## 3. Other hidden API changes handled

| API | Change | Status |
|---|---|---|
| `IPermissionManager.get/updatePermissionFlags` | `persistentDeviceId` argument (Android 14 QPR+) | new overloads tried first |
| `IPackageManager.checkSignatures` | `userId` argument (Android 13+) | new overload tried first |
| `IUidObserver.onUidProcAdjChanged` | added in Android 14 | implemented in the adapter |
| `IActivityManager.getDelegatedShellPermissions`, `stopDelegateShellPermissionIdentity`, `killApplication` | gained an `int` argument in 17.1 | not used by Shizuku |
| `IPermissionManager.grant/revokeRuntimePermission(..., String persistentDeviceId, ...)` | unchanged since Android 14 QPR3 | already supported |
| `IContentProvider.call(AttributionSource, ...)`, `ContentProvider.Transport.call` | unchanged | binder delivery path verified |
| `IActivityManager.getContentProviderExternal`, `registerProcessObserver`, `registerUidObserver`, `startActivityAsUser`, `forceStopPackage`, `checkPermission` | unchanged | |
| `IDeviceIdleController.addPowerSaveTempWhitelistApp(String, long, int, int, String)` | unchanged | |
| `ActivityThread.mBoundApplication` / `AppBindData` / `LoadedApk.makeApplication` (user services) | unchanged; `currentPackageName()` is null-safe on stock 17 | see §5 |

The complete list of framework references the APK makes that do not exist on
17.0/17.1, each reviewed as a guarded fallback, is
[`tools/hiddenapi-check/allowlist.txt`](../tools/hiddenapi-check/allowlist.txt).

## 4. Hidden API access from the manager app

The manager is a normal app and uses
[AndroidHiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass)
for hidden APIs (`SystemProperties`, `IUserManager` through
`ShizukuBinderWrapper`, ...). Version 6.2 (Oct 2026) fixes hidden field access
on Android 17 / 17.1; upstream Shizuku uses 6.1. 6.2 requires
`compileSdk 37`, so the fork compiles against API 37 while `targetSdk` stays
36 (see §6).

## 5. User services on OEM ROMs

Not Android 17 specific, but reported a lot together with it: some OEM
frameworks (HyperOS, Transsion, ...) read the bound application's process
name inside `LoadedApk.makeApplication()`, which throws in a process started
by `ActivityThread.systemMain()`, so `bindUserService()` fails
(RikkaApps/Shizuku#1198). Only if the normal call fails, the fork installs a
minimal `AppBindData` (process name, `appInfo`, `compatInfo`) and retries.
`appInfo` is mandatory because `ActivityThread.currentPackageName()`
dereferences it as soon as `mBoundApplication` is non-null.

## 6. Settings, wireless debugging and local network

* **`Settings.Global.ADB_ENABLED` and `DEVELOPMENT_SETTINGS_ENABLED` read as
  `0` for third-party apps** on Android 17 (`@Readable(redactedValue = "0")`,
  flag-gated; live on Pixels since the Sept 2026 update). Some forks gate the
  "Start" button on `ADB_ENABLED` and broke. Upstream Shizuku and this fork do
  not read it. `adb_wifi_enabled` is not redacted. If you need the real value
  in your own app, read it through Shizuku (`settings get global adb_enabled`
  runs as shell) - see [CLIENT_APPS.md](CLIENT_APPS.md).
* **adbd still listens on all interfaces.** The TLS connect server
  (`network_inaddr_any_server`) and the pairing server
  (`socket_inaddr_any_server`) in `packages/modules/adb` are unchanged in
  `android17-release`, so connecting to `127.0.0.1` keeps working. The fork
  additionally falls back to the device's own Wi-Fi address and uses connect
  timeouts, in case an OEM build only listens on Wi-Fi.
* **Local network permission** (`ACCESS_LOCAL_NETWORK`) is enforced only for
  apps that **target** API 37. The fork targets 36, so mDNS discovery of the
  wireless debugging service works as before. Forks that bumped `targetSdk` to
  37 had to add the permission to fix "endless pairing search".
* Wireless debugging only listens on Wi-Fi networks the user allowed: the
  "Allow wireless debugging on this network?" prompt must be accepted, or
  pairing/start finds nothing.
* Android 17 removed no permission from the shell (`packages/Shell`
  manifest, 16 QPR2 -> 17: 0 removed, 45 added), so ADB mode keeps the same
  power.

## 7. Samsung (One UI 9)

* **Auto Blocker** (Settings > Security and privacy > Auto Blocker) blocks
  USB debugging commands and, with "Maximum restrictions", wireless debugging.
  Turn it off while starting the service.
* Developer options > **Wireless debugging** must be on and the current
  Wi-Fi network allowed. Pairing uses the notification's reply field; keep
  the pairing code screen open in split-screen or pop-up view if the
  notification does not appear.
* Samsung's battery optimizer does not kill the service process (it is a
  shell process, not an app process), but a reboot does. Without root the
  service has to be started again after every reboot, or enable Settings >
  "Start on boot": on Android 13+ the manager (which the server grants
  `WRITE_SECURE_SETTINGS`) then waits for Wi-Fi, turns wireless debugging
  on and starts the service (`BootStartService`, a short foreground service;
  upstream tried for 3 seconds inside the boot receiver, usually before Wi-Fi
  was connected). The Wi-Fi network must have been allowed with "Always
  allow on this network".

## 8. Watch list: restricting on-device ADB

In July 2026 a Google engineer suggested in the adb feature request
[issuetracker 526109803](https://issuetracker.google.com/issues/526109803)
to bind adbd only to the Wi-Fi interface (`wlan0`), citing exploits through
the localhost socket (CVE-2026-0073, patched in May 2026, is related). That
would break every app that starts itself through wireless debugging on
`127.0.0.1` (Shizuku, LADB, libadb users). As of October 2026 nothing is
decided: no AOSP change, no bulletin, and `android17-release` still listens
on all interfaces (see §6).

What the fork already does about it:

* `AdbSockets.connect()` falls back to the device's own Wi-Fi address when
  loopback is refused, and mDNS discovery also probes the resolved address.
  That keeps working if adbd binds to the `wlan0` **address**. It would not
  help if adbd used `SO_BINDTODEVICE` (local traffic arrives on `lo`) or
  rejected local peers.
* Starting from a computer (`adb shell <starter path>`), root and Sui do not
  depend on loopback.

If the change lands it would most likely ship in the adbd Mainline module
(`com.google.android.adbd`, updated through Google Play system updates),
so watch `daemon/adb_wifi.cpp` in `platform/packages/modules/adb` and the
`service.adb.listen_addrs` property.

## What was not changed

* The Shizuku binder protocol, the `moe.shizuku.manager.permission.API_V23`
  permission, the `<package>.shizuku` provider and the `sendBinder` call are
  untouched. Every app built with `dev.rikka.shizuku:api` keeps working.
* `targetSdk` stays 36 on purpose (see §6). Moving to 37 later needs the local
  network permission flow for pairing.

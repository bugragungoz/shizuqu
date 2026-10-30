# What Android 17 broke, and the fixes

Shizuku 13.6.0 was built for Android 16. On Android 17 the service still
starts, but three things break:

* The list of authorized apps is empty.
* Apps that were already running when the service started never connect.
* Apps in other users (work profile, Samsung Secure Folder, Dual Messenger)
  never connect.

The fixes were checked against the real Android 17 framework: the 17.0
emulator image (`CE2A.260420.051.A3`), 17.1 (`CP31.260623.012`), and AOSP
`android17-release`. CI checks every build against the method signatures of
both builds in `tools/hiddenapi-check/frameworks`.

## Hidden API changes

| Android 17 change | Effect in Shizuku 13.6 | Fix |
|---|---|---|
| `IPackageManager.getInstalledPackages(long, int)` returns `PackageInfoList` instead of `ParceledListSlice` | `NoSuchMethodError` is swallowed into an empty list: the app list is empty and running apps get no binder | the new signature is called first (`IPackageManager37` via Refine), then the old one, then reflection |
| `IUserManager.getUsers(boolean, boolean, boolean)` was removed | only user 0 is seen | `getUsers(boolean)` on API 37 |
| The permission flag APIs take a `persistentDeviceId` (14 QPR3) or a `deviceId` (14 QPR2) | permission flags are not read or written | the new overloads are tried first |
| `IUidObserver.onUidProcAdjChanged` gained an `adj` argument | — | both forms are implemented |
| HiddenApiBypass 6.1 fails on some 17 / 17.1 fields | hidden API calls in the manager fail | updated to 6.2 |

Every reference the APK still makes to an old signature is a guarded
fallback, and each one is reviewed in
[`allowlist.txt`](../tools/hiddenapi-check/allowlist.txt).

## Other fixes

* **User services on OEM ROMs** (HyperOS and others): `makeApplication` reads
  the bound application, which does not exist in the server process. Only if
  the normal call fails, the fork sets a minimal `AppBindData` and tries again
  (RikkaApps/Shizuku#1198).
* **Start on boot without root:**
  * Upstream gave up after 3 seconds, which was usually before Wi-Fi
    connected.
  * Shizuqu uses a short foreground service instead. It waits for Wi-Fi,
    turns wireless debugging on, finds the port over mDNS and retries.
  * The same service is behind the Quick Settings tile.
* **Wireless debugging connection:**
  * The connection uses timeouts.
  * If adbd does not answer on `127.0.0.1`, the fork connects to the phone's
    own Wi-Fi address instead.
  * The mDNS discovery no longer gets stuck after a fast stop and start, or
    after an error.

## Things that did not change

* **adbd** still listens on all interfaces in `android17-release`. There is a
  proposal to bind it to `wlan0` only ([issue
  526109803](https://issuetracker.google.com/issues/526109803)), but nothing
  has been decided. The Wi-Fi address fallback above would keep working in
  that case.
* **Shell permissions:** Android 17 removed none of the shell user's
  permissions, so ADB mode has the same power as before.
* **`Settings.Global.ADB_ENABLED`** reads as `0` for normal apps on 17. Shizuku
  never read it.
* **The local network permission** applies only to apps that target API 37.
  Shizuqu targets 36, so mDNS discovery works as before.
* **The Shizuku protocol** (the permission, the provider, binder delivery) is
  unchanged. Every app built with `dev.rikka.shizuku:api` keeps working.

## Samsung (One UI 9)

* Turn off **Auto Blocker** while pairing and starting. It blocks debugging
  commands.
* If the pairing notification does not show up, open the pairing code screen
  in split-screen or pop-up view.
* For start on boot, the Wi-Fi network has to be allowed with "Always allow on
  this network".

# Changelog

## 13.7.0 (fork, unreleased)

Based on Shizuku 13.6.0 (upstream `b844bc49`).

### Android 17
- Authorized apps list is no longer empty: `getInstalledPackages()` returns
  `PackageInfoList` on Android 17.
- Apps that were running when the service started get the binder again.
- Apps in work profiles, Samsung Secure Folder and Dual Messenger are seen
  again: `IUserManager.getUsers(boolean, boolean, boolean)` was removed.
- HiddenApiBypass 6.2 (hidden API fixes for Android 17 / 17.1).
- Device-id aware permission flag APIs, `checkSignatures(..., userId)`,
  `IUidObserver.onUidProcAdjChanged`.

### Fixes
- User services: retry `makeApplication` with a bound application on OEM
  ROMs that need it (RikkaApps/Shizuku#1198).
- Wireless debugging: connect timeouts, fall back to the Wi-Fi address if
  adbd is not reachable on loopback, no crash on a null mDNS host.
- Start on boot without root (Android 13+): waits for Wi-Fi in a short
  foreground service and retries, instead of a 3 second window.

### Fork
- App name, package name, APK name, source URL and version in
  `branding.properties`; own launcher/notification icons.
- `api/` (Shizuku-API) and `hidden-api/` (HiddenApi) vendored as plain directories.
- `tools/hiddenapi-check`: hidden API linkage check against Android 17.0/17.1
  framework signatures, run in CI.
- compileSdk 37 (targetSdk 36), NDK 29.0.14206865.

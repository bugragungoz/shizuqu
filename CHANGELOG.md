# Changelog

## 13.7.0 (2026-10-30)

First release. It is based on Shizuku 13.6.0 (upstream `b844bc49`).

### Android 17
- The authorized apps list works again (`getInstalledPackages` returns
  `PackageInfoList`).
- Apps that were running when the service started can connect again.
- Apps in a work profile, Secure Folder or Dual Messenger are found again
  (`getUsers(boolean, boolean, boolean)` was removed).
- Device-id aware permission APIs are supported, and HiddenApiBypass is
  updated to 6.2.
- The server version is 13.7. The app asks you to restart a 13.6 server that
  is still running.

### New
- Start on boot without root (Android 13+). It waits for Wi-Fi, then turns
  wireless debugging on and starts the service. It is off by default.
- A Quick Settings tile shows whether the service runs and starts it with one
  tap. On the lock screen it asks you to unlock first.
- Copy log on the start screen, for bug reports.
- New design on Material 3:
  - Bugra palette by default, plus Material You and seven more palettes.
  - Geist and JetBrains Mono fonts.
  - A new icon.
- The new texts are translated into 14 languages.

### Fixes
- User services work on OEM ROMs that read the bound application
  (RikkaApps/Shizuku#1198).
- Wireless debugging:
  - The connection uses timeouts.
  - If adbd does not answer on loopback, the app uses the phone's own Wi-Fi
    address.
  - mDNS discovery no longer gets stuck after an error or a fast restart.
  - Found services are resolved one at a time.
- The app no longer crashes at launch when HiddenApiBypass fails.
- The app no longer crashes in Georgian (a broken translation).

### Project
- App name, package name and version are set in `branding.properties`.
- CI checks every build against the Android 17.0 and 17.1 framework
  (`tools/hiddenapi-check`), and runs tests and lint.
- `api/` and `hidden-api/` are plain directories, not submodules.

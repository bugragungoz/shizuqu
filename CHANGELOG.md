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
- Server version 13.7 (patch version 7), so the app asks to restart a
  server that is still the original 13.6.

### Fixes
- User services: retry `makeApplication` with a bound application on OEM
  ROMs that need it (RikkaApps/Shizuku#1198).
- Wireless debugging: connect timeouts, fall back to the Wi-Fi address if
  adbd is not reachable on loopback, no crash on a null mDNS host.
- Start on boot without root (Android 13+): waits for Wi-Fi in a short
  foreground service and retries, instead of a 3 second window.
- Georgian: the home screen no longer crashes when the server's version
  differs from the app's (an invalid format string in the translation).

### Reliability (review round)
- Start on boot is off by default (it turns wireless debugging on at boot).
- Boot start: any connected Wi-Fi counts (not only the default network),
  fits the ~3 minute short-service limit, stops with its start id, socket
  read timeout.
- mDNS discovery: no leaked discovery or listener reuse on fast start/stop
  or a failed stop; a failed start can be retried; found services are
  resolved one at a time (Android 11-13 reject parallel resolves, which
  could hide this phone's adbd when another phone on the Wi-Fi announces
  adb too).
- The manager no longer crashes at launch if HiddenApiBypass fails.
- User service OEM fallback only for NullPointerException, and sets
  `AppBindData.info`.
- `clean` + build in one Gradle call no longer loses the branding.
- Hidden API check also fails on unknown missing classes.
- Boot start: the TLS connection to adbd has a read timeout too, and a
  start request at the end of an attempt can no longer leave the service
  in the foreground with nothing running.

### Design
- Bugra design system on Shizuku's layout: warm black and Android green
  palette by default, Geist and JetBrains Mono type, 14dp flat cards with a
  hairline border, one filled action per card.
- Color palettes in Settings: Bugra, Material You (Android 12+), Indigo,
  Ocean, Teal, Violet, Rose, Amber, Graphite; each in light and dark.
- Opt-in screenshot tests (`-Pscreenshots`) for reviewing the design without
  a device.

### Additions
- Quick Settings tile: shows whether the service runs; a tap starts it
  through wireless debugging (Android 13+, after a first start from the
  app), otherwise opens the app. Asks to unlock the phone first.
  Settings > "Add Quick Settings tile" adds it (Android 13+).
- Start screen: "Copy log" copies the start log with the app, Android and
  device versions, ready for a bug report.
- Releases: pushing a `v*` tag creates a draft GitHub release with the
  signed APK and its checksums.
- The new texts are translated into 13 languages (German, Spanish, French,
  Italian, Brazilian Portuguese, Russian, Ukrainian, Polish, Chinese,
  Japanese, Korean, Indonesian, Turkish).

### Fork
- Icon: a terminal window with Android's antennae and a `>_` prompt, as
  launcher (adaptive and themed), status bar and permission dialog icon.
  Generated from one geometry by `tools/icon/gen.py`.
- Release key in `signing/release.p12` (encrypted); CI needs only the
  `KEYSTORE_PASSWORD` secret.
- App name, package name, APK name, source URL and version in
  `branding.properties`; own launcher/notification icons.
- `api/` (Shizuku-API) and `hidden-api/` (HiddenApi) vendored as plain directories.
- `tools/hiddenapi-check`: hidden API linkage check against Android 17.0/17.1
  framework signatures, run in CI.
- CI also runs lint (0 errors) and Robolectric tests (every screen in every
  palette, the tile's start rules, mDNS discovery).
- compileSdk 37 (targetSdk 36), NDK 29.0.14206865.

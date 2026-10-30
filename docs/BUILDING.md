# Building

## Requirements

* JDK 21
* Android SDK with `platforms;android-37.0`, `build-tools;36.0.0`,
  `ndk;29.0.14206865`, `cmake;3.31.6`

```bash
sdkmanager "platforms;android-37.0" "build-tools;36.0.0" "ndk;29.0.14206865" "cmake;3.31.6"
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :manager:assembleDebug     # or :manager:assembleRelease
```

APKs: `manager/build/outputs/apk/<debug|release>/`. Release builds are also
copied to `out/apk/`.

`compileSdk` is 37 (needed by HiddenApiBypass 6.2), `targetSdk` is 36. AGP
8.10 warns that it was not tested with API 37; the warning is suppressed in
`gradle.properties`.

## Signing

Android only installs an update over an existing install when it is signed
with the same key. The release key is in the repository as
[`signing/release.p12`](../signing/README.md), encrypted with AES-256 under a
28-character random password that is **not** in the repository.

Local release build: create `signing.properties` (git-ignored) in the
repository root:

```properties
KEYSTORE_FILE=../signing/release.p12
KEYSTORE_PASSWORD=<password>
KEYSTORE_ALIAS=release
KEYSTORE_ALIAS_PASSWORD=<password>
```

(`KEYSTORE_FILE` is relative to `manager/`.) Without `signing.properties`,
both build types are signed with the local debug key.

## GitHub Actions

`.github/workflows/app.yml` builds on every push, checks that the label is
`app.name` with no "Shizuku" left in the strings, checks the APK against the
Android 17.0 and 17.1 framework signatures and uploads the APKs as a build
artifact (Actions tab > run > Artifacts).

To sign CI release APKs with the release key, add **one** repository secret
(Settings > Secrets and variables > Actions > New repository secret):

| Secret | Value |
|---|---|
| `KEYSTORE_PASSWORD` | the password of `signing/release.p12` |

Without it, CI release APKs are signed with the runner's throw-away debug key
(and cannot update an installed release). To use a different key instead,
also set `KEYSTORE` (`base64 -w0 your.jks`), `KEYSTORE_ALIAS` and
`KEYSTORE_ALIAS_PASSWORD`.

## Releases

1. Set the version in `branding.properties` (`app.versionBase`) and give the
   `CHANGELOG.md` section that version as its heading (`## 13.7.0 ...`).
2. Tag and push: `git tag v13.7.0 && git push origin v13.7.0`.
3. CI builds, checks that the APK is signed with `signing/release.p12` (the
   job fails otherwise, since another key cannot update installed copies),
   and creates a **draft** GitHub release with the APK, its SHA-256 and that
   CHANGELOG section. Review it and publish it on GitHub.

## Hidden API check locally

```bash
python3 tools/hiddenapi-check/check.py check manager/build/outputs/apk/debug/*.apk \
    tools/hiddenapi-check/frameworks/android-37.0.sigs.gz \
    --only android/,com/android/ --allow tools/hiddenapi-check/allowlist.txt
```

Needs `dexdump` from build-tools (found through `ANDROID_HOME`).

## Unit and smoke tests

```bash
./gradlew :manager:testDebugUnitTest
```

`AdbMdnsTest` drives the wireless debugging port discovery through NsdManager's
asynchronous start/stop callbacks (stop before start confirmed, restart while
stopping, failed start or stop). `LaunchSmokeTest` (Robolectric, no device)
opens the home screen in every palette, light and
dark, and checks that each palette's primary color really applies (except
Material You, which follows the wallpaper); it also opens Settings, the app
list and the two guides, and checks when the Quick Settings tile may start
the service (never for root users or without the grant). A theme or layout
mistake that would crash the app at launch fails here (and in CI) instead of
on a phone.

## Screenshot tests (design review)

```bash
./gradlew :manager:recordPaparazziDebug -Pscreenshots   # write images
./gradlew :manager:verifyPaparazziDebug -Pscreenshots   # compare with them
```

They render the real layouts and themes with layoutlib (no device or
emulator). The tests are in `manager/src/screenshotTest/` (compiled only with
`-Pscreenshots`), the images in `manager/src/test/snapshots/images/`: home cards, permission
dialog, starter log, palette picker, every palette in light and dark, and the
icon. Review the images in a diff after changing colors, type, shapes or
layouts.

`-Pscreenshots` switches to compileSdk 35 and HiddenApiBypass 6.1, because
layoutlib does not know API 37 yet. Never build a release with it.

Paparazzi's layoutlib cannot inflate `<adaptive-icon>` or `MaterialSwitch`;
the tests draw the icon's layers themselves and leave the switch rows out.

## Icon

`tools/icon/gen.py` (needs `pip install shapely`) writes every icon drawable
from one geometry; colors are in `res/values/launcher_colors.xml`. See
[RENAMING.md](RENAMING.md#icon).

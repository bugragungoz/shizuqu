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

Without `signing.properties`, both build types are signed with the local
debug key. Android only installs an update over an existing install when the
signing key is the same, so create **one** release key and keep it safe -
losing it means users have to uninstall (and re-authorize every app).

```bash
keytool -genkeypair -v -keystore release.jks -alias release \
    -keyalg RSA -keysize 4096 -validity 36500
```

`signing.properties` (git-ignored) in the repository root:

```properties
KEYSTORE_FILE=../release.jks
KEYSTORE_PASSWORD=...
KEYSTORE_ALIAS=release
KEYSTORE_ALIAS_PASSWORD=...
```

(`KEYSTORE_FILE` is relative to `manager/`.) Never commit the keystore.

## GitHub Actions

`.github/workflows/app.yml` builds on every push, checks the APK against the
Android 17.0 and 17.1 framework signatures and uploads the APKs as a build
artifact (Actions tab > run > Artifacts).

To get release APKs signed with your key, add these repository secrets
(Settings > Secrets and variables > Actions):

| Secret | Value |
|---|---|
| `KEYSTORE` | `base64 -w0 release.jks` |
| `KEYSTORE_PASSWORD` | store password |
| `KEYSTORE_ALIAS` | `release` |
| `KEYSTORE_ALIAS_PASSWORD` | key password |

Without them, CI release APKs are signed with the runner's throw-away debug
key, so each CI build would need a reinstall.

## Hidden API check locally

```bash
python3 tools/hiddenapi-check/check.py check manager/build/outputs/apk/debug/*.apk \
    tools/hiddenapi-check/frameworks/android-37.0.sigs.gz \
    --only android/,com/android/ --allow tools/hiddenapi-check/allowlist.txt
```

Needs `dexdump` from build-tools (found through `ANDROID_HOME`).

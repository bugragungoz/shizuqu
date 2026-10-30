# Development

## Build

You need JDK 21 and these Android SDK packages: `platforms;android-37.0`,
`build-tools;36.0.0`, `ndk;29.0.14206865` and `cmake;3.31.6`.

```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :manager:assembleRelease
```

The APKs are written to `manager/build/outputs/apk/`. The build uses
`compileSdk` 37 because HiddenApiBypass 6.2 needs it, and keeps `targetSdk`
at 36 on purpose: targeting 37 would need the local network permission for
pairing.

## Checks

Before a pull request or a release, run:

```bash
./gradlew :manager:testDebugUnitTest :manager:assembleRelease :manager:lintRelease
```

These are the same checks CI runs on every push, plus the hidden API check
and the branding check:

* **Tests.** Robolectric runs them without a device. They open every screen in
  every palette and check the Quick Settings tile's start rules
  (`LaunchSmokeTest`). They also check the mDNS discovery of the wireless
  debugging port (`AdbMdnsTest`).
* **Lint.** It must report 0 errors. Missing translations are only warnings.
* **Screenshots.** Run
  `./gradlew :manager:recordPaparazziDebug -Pscreenshots`; the images go to
  `manager/src/test/snapshots/images/`. This mode builds with compileSdk 35,
  so never use it for a release.

## Signing

Android installs an update only if it is signed with the same key.

* The release key is `signing/release.p12`. It is encrypted, and its password
  is not in the repository.
* For local builds, create a `signing.properties` file in the repository
  root. It is ignored by git.

  ```properties
  KEYSTORE_FILE=../signing/release.p12
  KEYSTORE_PASSWORD=<password>
  KEYSTORE_ALIAS=release
  KEYSTORE_ALIAS_PASSWORD=<password>
  ```

* Only release APKs use this key. Debug APKs are signed with the local debug
  key, so they cannot be installed over a release.
* For CI, add the repository secret `KEYSTORE_PASSWORD`. Without it, CI signs
  release APKs with a throw-away key, and those cannot update an installed
  copy.

## Releases

1. Set `app.versionBase` in `branding.properties`.
2. Add a `## <version>` section to `CHANGELOG.md`.
3. Push a tag (`git tag v13.7.1 && git push origin v13.7.1`), or open
   Actions > App > Run workflow and check "release".

When the tag arrives, CI checks that the APK is signed with the release key
and publishes a GitHub release with the APK, its SHA-256 and the CHANGELOG
section. This needs the `KEYSTORE_PASSWORD` secret.

## Name, package and icon

All branding is in `branding.properties`:

| Key | Used for |
|---|---|
| `app.name` | the app's label; it also replaces every "Shizuku" in the UI strings |
| `app.applicationId` | the package name; the default `moe.shizuku.privileged.api` lets apps that look Shizuku up by package name find it |
| `app.fileName`, `app.versionBase`, `app.sourceUrl` | the APK name, the version, and the "source code" link |

To change the icon:

* Colors are in `manager/src/main/res/values/launcher_colors.xml`.
* The shape comes from `tools/icon/gen.py`. Install shapely first
  (`pip install shapely`).

Never rename what the Shizuku protocol depends on:

* the `moe.shizuku.manager.permission.API_V23` permission
* the `<package>.shizuku` provider authority
* the binder descriptor and transaction codes (`ShizukuApiConstants`)
* the `rikka.shizuku.intent.action.REQUEST_BINDER` action

Upstream does not allow publishing an APK named "Shizuku", or one that
declares the `moe.shizuku.manager.permission.*` permissions, to an app store.
In practice that means distributing through GitHub only.

## A new Android version

Shizuku depends on hidden APIs, so a new Android version can break it. To
check a new version:

1. Get its `framework.jar`, either with
   `adb pull /system/framework/framework.jar` or from the emulator image.
2. Dump its method signatures and check the APK against them:

   ```bash
   python3 tools/hiddenapi-check/check.py dump-framework framework.jar android-38.0.sigs
   gzip -9 android-38.0.sigs && mv android-38.0.sigs.gz tools/hiddenapi-check/frameworks/
   python3 tools/hiddenapi-check/check.py check manager/build/outputs/apk/release/*.apk \
       tools/hiddenapi-check/frameworks/android-38.0.sigs.gz \
       --only android/,com/android/ --allow tools/hiddenapi-check/allowlist.txt
   ```

3. Fix every reference that is "NOT FOUND" in `hidden-api/`:
   * Try the new signature first and fall back on `NoSuchMethodError`.
   * If only the return type changed, declare the new one in a separate
     interface with `@RefineAs`. `IPackageManager37` is an example.
   * Keep the old calls as guarded fallbacks, list them in `allowlist.txt`,
     and note the change in `hidden-api/PATCHES.md`.
4. Read the "behavior changes" pages for the new version.
5. Bump `SERVER_PATCH_VERSION` in `ShizukuApiConstants` when the server
   changes. The app then asks the user to restart an older running server.

The Android 17 changes are described in [ANDROID17.md](ANDROID17.md).

## Merging upstream

```bash
git remote add upstream https://github.com/RikkaApps/Shizuku.git
git fetch upstream && git merge upstream/master
```

`api/` (Shizuku-API) and `hidden-api/` (HiddenApi) are plain directories in
this repository, not submodules. To take an upstream change into them, apply
its diff by hand.

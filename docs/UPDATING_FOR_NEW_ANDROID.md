# Maintenance runbook: a new Android version (or QPR) is out

Shizuku lives on hidden APIs, so every Android release can break it. This is
the procedure that found the Android 17 breakages; repeat it for Android 18,
QPRs and betas.

## 1. Get the new `framework.jar`

Either from a phone running it:

```bash
adb pull /system/framework/framework.jar framework-38.jar
```

or from the emulator image (no device needed):

```bash
sdkmanager --list | grep "system-images;android-38"
# download e.g. system-images;android-38.0;google_apis;x86_64, then
#   system.img (GPT) -> "super" partition -> logical partition "system" (ext4 or erofs)
#   -> /system/framework/framework.jar
```

`system.img` of recent images is a GPT disk with a `super` partition. The
`system` logical partition inside it can be cut out with `lpunpack` (or a few
lines of Python reading the LP metadata at offset 4096 of the super
partition), then read with `debugfs` (ext4) or `fsck.erofs --extract` (erofs).

## 2. Dump its signatures and check the APK

```bash
python3 tools/hiddenapi-check/check.py dump-framework framework-38.jar framework-38.sigs
gzip -9 framework-38.sigs   # commit as tools/hiddenapi-check/frameworks/android-38.0.sigs.gz

./gradlew :manager:assembleRelease
python3 tools/hiddenapi-check/check.py check manager/build/outputs/apk/release/*.apk \
    tools/hiddenapi-check/frameworks/android-38.0.sigs.gz \
    --only android/,com/android/ --allow tools/hiddenapi-check/allowlist.txt
```

Every line under "NOT FOUND" is a method the app calls that does not exist on
that build. For each one:

1. Find the caller (printed below the reference).
2. Find the new signature in the dump:
   `zgrep "IPackageManager; getInstalledPackages" tools/hiddenapi-check/frameworks/*.gz`
3. Read the AIDL in AOSP (`android.googlesource.com/platform/frameworks/base/+/refs/heads/android18-release/...`)
   to learn what the new arguments mean.
4. Fix it in `hidden-api/`:
   * new **arguments**: add the overload to the stub interface
     (`hidden-api/stub/...`) and try it first in the compat class, falling
     back on `NoSuchMethodError`;
   * new **return type**: declare it in a separate interface annotated with
     `@RefineAs(IOriginal.class)` (see `IPackageManager37`), because Java does
     not allow two methods that only differ in return type;
   * method **removed**: use the replacement, keep the old call as fallback.
5. If the old reference stays as a guarded fallback, add it to
   `allowlist.txt`, with `requires <new signature>` when the platform changed
   it, so the check fails if the new path is ever lost.

Also diff the method lists between two builds to spot changes in classes the
app uses through reflection (those are invisible to the checker):

```bash
diff <(zcat android-37.1.sigs.gz | grep "Landroid/app/LoadedApk;") \
     <(zcat android-38.0.sigs.gz | grep "Landroid/app/LoadedApk;")
```

Reflection targets worth diffing: `ActivityThread`, `ActivityThread$AppBindData`,
`LoadedApk`, `ContextImpl` (user services), `ParceledListSlice`.

## 3. Read the behavior-change pages

* `developer.android.com/about/versions/<N>/behavior-changes-all`
* `.../behavior-changes-<N>` (only when raising `targetSdk`)

Things that hurt Shizuku before: settings redaction (`adb_enabled`),
local-network permission, background activity starts, foreground service
types, package visibility, `app_process`/SELinux changes.

## 4. Check what ADB (shell) can still do

```bash
for b in android17-release android18-release; do
  curl -s "https://android.googlesource.com/platform/frameworks/base/+/refs/heads/$b/packages/Shell/AndroidManifest.xml?format=TEXT" \
    | base64 -d | grep -o 'uses-permission android:name="[^"]*"' | sort -u > shell-$b.txt
done
comm -3 shell-android17-release.txt shell-android18-release.txt
```

## 5. Wireless debugging

Read `daemon/adb_wifi.cpp` and `pairing_connection/pairing_server.cpp` in
`platform/packages/modules/adb` for the new branch: if the servers stop
listening on all interfaces, loopback connections break (the fork already
falls back to the Wi-Fi address, see `AdbSockets.kt`).

## 6. Merging upstream Shizuku

```bash
git remote add upstream https://github.com/RikkaApps/Shizuku.git   # once
git fetch upstream
git merge upstream/master
```

`api/` and `hidden-api/` are plain directories here (no submodules). To take
changes from RikkaApps/Shizuku-API or RikkaW/HiddenApi, diff them against the
commit noted in the vendoring commit message and apply the diff.

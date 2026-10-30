<p align="center"><img src="docs/assets/icon.svg" width="96" height="96" alt=""></p>

<h1 align="center">Shizuqu</h1>

<p align="center">Shizuku for Android 17. Same API, same apps.</p>

<p align="center"><a href="https://github.com/bugragungoz/shizuqu/actions/workflows/app.yml"><img src="https://github.com/bugragungoz/shizuqu/actions/workflows/app.yml/badge.svg" alt="Build"></a></p>

On Android 17, [Shizuku](https://github.com/RikkaApps/Shizuku) shows an empty
list of authorized apps and apps can no longer connect to it. Shizuqu is a fork
that fixes this. Apps that use the Shizuku API work with it unchanged.

[Türkçe](docs/README_TR.md)

<p align="center">
  <img src="manager/src/test/snapshots/images/moe.shizuku.manager.screenshots_DesignScreenshots_home_bugra_dark.png" width="250" alt="Home screen, dark">
  <img src="manager/src/test/snapshots/images/moe.shizuku.manager.screenshots_DesignScreenshots_home_bugra_light.png" width="250" alt="Home screen, light">
  <img src="manager/src/test/snapshots/images/moe.shizuku.manager.screenshots_DesignScreenshots_palettes_dark.png" width="250" alt="Color palettes">
</p>

## Download

**[Shizuqu 13.7.0](release/Shizuqu-v13.7.0.apk)** (Android 7.0+)
SHA-256 `f5eefcdc6e72febe5d01ac5c899dde8edebcb79c912dc58edbbdeeda210c1521`

Uninstall the original Shizuku first. Both use the same package name with
different signatures, so they cannot be installed side by side.

## Start the service

* **Without root (Android 11+):** turn on Developer options > Wireless
  debugging and allow the Wi-Fi network. Then tap Pair in the app, and then
  Start.
* **Root:** tap Start on the root card.
* **From a computer:** `adb shell` followed by the command shown in the app.

Without root, the service stops on every reboot. You can start it again with
one tap from the Quick Settings tile (Android 13+), or turn on Settings >
Start on boot. On Samsung phones, turn off Auto Blocker while pairing.

## What is different from Shizuku

* **Android 17 fixes:** the authorized apps list works again. Apps that were
  already running when the service started can connect. Apps in a work
  profile, Secure Folder or Dual Messenger are found. Details are in
  [docs/ANDROID17.md](docs/ANDROID17.md).
* **Start on boot without root**, and a **Quick Settings tile**.
* **New design** on Material 3:
  * Material You plus eight color palettes. The default is Bugra: warm black
    with Android green.
  * Geist and JetBrains Mono fonts.
  * A new icon.
* **Copy log** on the start screen, for bug reports.
* **Checked in CI:** every build is checked against the Android 17.0 and 17.1
  framework, with tests and lint.

## Safety

Without root, the service has the same rights as `adb shell`. It cannot
write to the system partitions, the bootloader or Knox, and a reboot stops
it. Apps you allow get these same rights, so only allow apps you trust.
Uninstalling Shizuqu undoes everything.

## For developers

* Apps that use Shizuku: [docs/CLIENT_APPS.md](docs/CLIENT_APPS.md)
* Building, signing, renaming, and the next Android version:
  [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)
* All changes: [CHANGELOG.md](CHANGELOG.md)

## Credits

* [Shizuku](https://github.com/RikkaApps/Shizuku) by RikkaApps and its
  contributors, the app this fork is based on.
* [Shizuku-API](https://github.com/RikkaApps/Shizuku-API) and
  [HiddenApi](https://github.com/RikkaW/HiddenApi) by RikkaW.
* [HiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass) by LSPosed.
* The [Geist](https://github.com/vercel/geist-font) and
  [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono) fonts (SIL OFL 1.1).
* The Shizuku translators.
* Built with [Claude Code](https://claude.com/claude-code) (Anthropic).

## License

Apache License 2.0, the same as upstream; `hidden-api/` is MIT. See
[NOTICE](NOTICE).

"Shizuku" and its icons belong to the upstream project. Shizuqu has its own
name and icon, and is distributed only through GitHub.

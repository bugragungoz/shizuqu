# Contributing

Bug reports and pull requests are welcome.

* **Bugs**: use the bug report template. For start problems, "Copy log" on
  the start screen copies everything needed.
* **Security issues**: privately, see [SECURITY.md](SECURITY.md).
* **Apps that use Shizuku**: a problem that only one app has is usually that
  app's; [docs/CLIENT_APPS.md](docs/CLIENT_APPS.md) lists what changed for
  them on Android 17.

## Pull requests

1. Build and test locally (setup in [docs/BUILDING.md](docs/BUILDING.md)):

   ```bash
   ./gradlew :manager:testDebugUnitTest :manager:assembleRelease :manager:lintRelease
   ```

   For UI changes, also look at the screenshots:
   `./gradlew :manager:recordPaparazziDebug -Pscreenshots`.
2. Keep each commit to one change, with a message that says why.
3. Hidden API changes: run `tools/hiddenapi-check` against the framework
   dumps (CI does it too) and update
   [hidden-api/PATCHES.md](hidden-api/PATCHES.md).
4. Do not rename what the Shizuku protocol depends on (permission, provider
   authority, binder descriptor, ...); see
   [docs/RENAMING.md](docs/RENAMING.md#what-must-not-be-renamed).

## Style

* Kotlin/Java: follow the surrounding code.
* User-facing text: sentence case, plain words, no exclamation marks.
  English in `values/strings.xml`; translations are welcome for any
  language.
* Design: Material 3 roles and the text appearances in
  `res/values/design_bugra.xml`, no hard-coded colors or fonts in layouts.

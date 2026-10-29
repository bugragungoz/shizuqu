# Renaming the app / the repository

Everything user-visible comes from [`branding.properties`](../branding.properties):

| Key | Used for |
|---|---|
| `app.name` | launcher label and every "Shizuku" in the UI strings (all languages) |
| `app.applicationId` | Android package name; also passed to the server, rish, the user-service starter and the native starter |
| `app.fileName` | APK file name (`<fileName>-v<version>-<buildType>.apk`) |
| `app.sourceUrl` | "View source code" link in the About dialog |
| `app.versionBase` | user-visible version (`<versionBase>.r<commits>.<hash>`) |

After editing it: `./gradlew clean :manager:assembleRelease`.

## Choosing the application id

* `moe.shizuku.privileged.api` (default): drop-in replacement for Shizuku.
  Apps that look for Shizuku by package name keep working. You must uninstall
  the original Shizuku first (same package, different signature).
* Your own id (e.g. `io.github.<you>.<name>`): all apps using the Shizuku API
  still work (binder delivery and permissions do not depend on the manager's
  package name), but apps that hard-code `moe.shizuku.privileged.api` to open
  the manager or to check that it is installed will not find it.

Either way the original Shizuku has to be uninstalled first. Both apps declare
`moe.shizuku.manager.permission.API_V23` (and `.MANAGER`), which every Shizuku
client requests, and Android refuses to install a second app with a different
signature that declares the same permission
(`INSTALL_FAILED_DUPLICATE_PERMISSION`). Renaming the permission would break
every existing client, so it stays.

Changing the id later is a new app for Android: authorizations granted in the
old one are not carried over.

## What must NOT be renamed

These are part of the protocol every Shizuku client speaks:

* the `moe.shizuku.manager.permission.API_V23` permission (and its group)
* the `<package>.shizuku` provider authority and the `sendBinder` /
  `sendUserService` calls
* the `moe.shizuku.privileged.api.intent.extra.BINDER` extra key
* `rikka.shizuku.intent.action.REQUEST_BINDER` (rish)
* `ShizukuApiConstants` (binder descriptor, transaction codes)
* Java package names (`moe.shizuku.*`, `rikka.shizuku.*`) - internal only,
  renaming them makes upstream merges painful for no user-visible gain

## Renaming the GitHub repository

GitHub redirects the old URL. Update `app.sourceUrl` and the clone URLs in
the README.

## Before making it public or publishing it

Upstream's README (Apache License 2.0, section 6 - trademarks) forbids:

* using upstream's `manager/src/main/res/mipmap*/ic_launcher*.png` icons -
  this fork ships its own icons and removed those files;
* distributing to **app stores** (Google Play, F-Droid, ...) an APK that uses
  "Shizuku" as its name, `moe.shizuku.privileged.api` as application id, or
  declares `moe.shizuku.manager.permission.*` permissions.

The last point means a store build has to use its own application id - and
then it cannot declare `moe.shizuku.manager.permission.API_V23`, which every
Shizuku client requests. Store distribution of a Shizuku-compatible manager
is therefore effectively not allowed; GitHub releases are fine.

Keep [`NOTICE`](../NOTICE) and the license files (`LICENSE`, `api/LICENSE`,
`hidden-api/LICENSE`).

# Safety: what Shizuqu can and cannot do to your phone

## Short answer

Shizuqu cannot brick a phone. Without root, the service runs as the `shell`
user (uid 2000), the same identity `adb shell` has. Android does not let that
user write the system, vendor or boot partitions, touch the bootloader, or
change anything that survives a factory reset. It does not root the phone,
unlock the bootloader or trip Samsung Knox. A reboot stops the service
completely (unless you turn on "Start on boot").

## Exactly what it changes

| What | When | How to undo |
|---|---|---|
| Starts one background process, `shizuku_server` (shell user) | when you tap Start | menu > Stop, or reboot |
| Grants itself `WRITE_SECURE_SETTINGS` | first start (same as upstream Shizuku) | uninstall the app |
| `adb_wifi_enabled = 1`, `adb_enabled = 1` (turns wireless / USB debugging on) | when you open the wireless "Start" dialog or tap the Quick Settings tile (after unlocking; only if you last started through adb), and at boot if "Start on boot" is on | Developer options |
| `adb_allowed_connection_time = 0` (ADB authorizations do not expire after 7 days of disuse) | same as above, so the paired key keeps working | Developer options > "Revoke USB debugging authorizations" |
| `/data/user_de/0/com.android.shell/shizuku.json` (which apps you allowed) | when you allow / deny an app | deny the app, or clear Shell's data |
| Grants / revokes the Shizuku permission of apps that request it | when you allow / deny an app | the app list in Shizuqu |
| At boot, waits up to ~3 minutes for Wi-Fi and starts the service | only if you enabled **Settings > Start on boot** (off by default) | turn it off |

Nothing else. The manager app itself is a normal app with no root access.

## The real risk: the apps you authorize

Every app you allow gets shell rights. Shell can, for example, uninstall or
disable system apps for your user (`pm uninstall --user 0`,
`pm disable-user`), change settings and grant permissions. A debloater that
removes the wrong system package can leave the phone in a broken state (for
example no launcher or a crashing System UI). This is true for upstream
Shizuku and every fork.

* Only allow apps you trust, and only while you use them.
* With debloaters, never remove packages you don't recognize.
* Packages removed with `pm uninstall --user 0` stay on the system partition and
  come back with `cmd package install-existing <package>` or a factory reset.

## If something goes wrong with Shizuqu itself

1. Reboot: the service is gone.
2. Uninstall Shizuqu: everything it added goes with it (the small config file
   in Shell's data is harmless).
3. Turn off Developer options (this also turns wireless debugging off) and use
   "Revoke USB debugging authorizations".

## How this build was checked

* Every hidden framework method/field the APK references was checked against
  the real Android 17.0 and 17.1 framework (`tools/hiddenapi-check`, also in
  CI). The only missing ones are guarded fallbacks for older Android
  versions, listed and justified in `tools/hiddenapi-check/allowlist.txt`.
* The reflection targets (user services) were checked against the same
  framework builds.
* Changes against upstream Shizuku 13.6.0 are small and listed per commit;
  see [ANDROID17.md](ANDROID17.md).
* It has not been run on a physical Android 17 device by the maintainers yet;
  the first device test is yours. The worst outcome of a bug is that Shizuqu
  or an app using it does not work, not damage to the phone.

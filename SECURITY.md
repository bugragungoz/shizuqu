# Security

Shizuqu runs a service with `adb shell` (or root) rights and hands access to
it to the apps you allow, so security bugs matter more here than in a usual
app.

## Reporting a vulnerability

Do not open a public issue. Use **Security > Report a vulnerability** on the
GitHub repository (a private advisory), with:

* what an attacker can do, and what they need first (an installed app, a
  paired computer, physical access, ...),
* steps or a proof of concept,
* the Shizuqu version (menu > About) and the Android version.

Reports are answered as soon as possible. Fixes ship in a new release, with
credit if you want it.

## In scope

* An app getting Shizuqu access without the user allowing it, or keeping it
  after being denied.
* The service accepting binder calls it should refuse, or running code for
  a caller that is not allowed.
* The manager turning on wireless debugging, or starting the service,
  without a user action (start on boot and the Quick Settings tile are user
  actions; the tile asks to unlock first).
* Leaks of the adb key or of the pairing code.

## Out of scope

* What an allowed app does with shell rights: that is the point of
  Shizuku-compatible apps. Only allow apps you trust.
* Problems of upstream Shizuku that this fork does not change: report them
  to [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku) too.

What the app changes on a phone and how to undo it:
[docs/SAFETY.md](docs/SAFETY.md).

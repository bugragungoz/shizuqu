# Security

Shizuqu gives the apps you allow `adb shell` (or root) rights, so security
bugs matter here.

Please report vulnerabilities privately, through **Security > Report a
vulnerability** on GitHub, not in a public issue. A report should include:

* what an attacker can do, and what they need first
* the steps to reproduce it
* the Shizuqu version and the Android version

These are in scope:

* An app getting access without being allowed, or keeping it after being
  denied.
* The service running calls for a caller that is not allowed.
* Wireless debugging being turned on, or the service started, without a user
  action.
* A leak of the adb key or the pairing code.

What an allowed app does with shell rights is out of scope: that is what
those apps are for.

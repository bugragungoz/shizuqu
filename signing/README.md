# Release signing key

`release.p12` signs Shizuqu's release APKs (alias `release`). Its certificate
SHA-256 is:

`e449ece69b5a674e22ef0386a9ff901ec4b1565bb889da58f0239323e6d801e6`

The file is encrypted with AES-256, and its password is not in this
repository. Never replace this file: an APK signed with a different key cannot
update an installed copy.

To build signed APKs locally or in CI, see
[docs/DEVELOPMENT.md](../docs/DEVELOPMENT.md#signing).

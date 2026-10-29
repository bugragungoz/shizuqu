# Release signing key

`release.p12` is the key that signs Shizuqu release APKs (alias `release`,
certificate SHA-256 `e449ece69b5a674e22ef0386a9ff901ec4b1565bb889da58f0239323e6d801e6`).

It is a PKCS#12 file encrypted with AES-256 (PBKDF2-HMAC-SHA256) under a
28-character random password, so it is useless without that password. The
password is **not** in this repository. Keep it in a password manager and in
the `KEYSTORE_PASSWORD` GitHub Actions secret.

Android installs an update only if it is signed with the same key, so never
replace this file: a new key means users must uninstall (and re-authorize all
apps) to update.

* CI: add the repository secret `KEYSTORE_PASSWORD`; release APKs are then
  signed with this key (see `.github/workflows/app.yml`).
* Local build: create `signing.properties` in the repository root (git-ignored):

  ```properties
  KEYSTORE_FILE=../signing/release.p12
  KEYSTORE_PASSWORD=<password>
  KEYSTORE_ALIAS=release
  KEYSTORE_ALIAS_PASSWORD=<password>
  ```

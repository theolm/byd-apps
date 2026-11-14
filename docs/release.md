# Release

Releases are built by [`.github/workflows/release.yml`](../.github/workflows/release.yml)
on tags matching `vX.Y.Z` (or manually with an existing tag). The workflow
builds both apps, verifies the APK signatures and publishes a GitHub release.

## Keystore and secrets

The workflow expects a release keystore supplied through the following
environment secrets in the `release` environment:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64 of `release.keystore` |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias |
| `ANDROID_KEY_PASSWORD` | Key password |

Create the environment in **Settings → Environments** and add the secrets
there. The `android` job declares `environment: release`, so these values are
only available to that job; repository-level secrets are not required. Add
required reviewers in the environment settings when the plan allows it.

Create the keystore once and keep it safe. Losing it means a new signing key
and a one-time uninstall on every car:

```bash
mkdir -p keystore
keytool -genkeypair -v \
  -keystore keystore/release.keystore \
  -alias bydapps \
  -keyalg RSA -keysize 4096 -validity 10000
base64 -i keystore/release.keystore | pbcopy   # macOS
```

`*.keystore` is gitignored; never commit it.

## Local signed build

Set the same values as environment variables and run:

```bash
KEYSTORE_PATH=keystore/release.keystore \
KEYSTORE_PASSWORD=... \
KEY_ALIAS=bydapps \
KEY_PASSWORD=... \
./gradlew :apps:youtube:assembleRelease :apps:ytmusic:assembleRelease \
  -PversionName=0.1.0 -PversionCode=1
```

Without a keystore, the release APKs are unsigned; the debug APKs still work.

## Version and tag

- Update `versionName` in `gradle/libs.versions.toml` and commit it.
- Tag the commit with the same version: `git tag v0.1.0 && git push origin v0.1.0`.
- The workflow uses `GITHUB_RUN_NUMBER` as the Android `versionCode`, so every
  run has a higher code.

The two APKs are published as:

- `YouTubeBYD-<version>-android-arm64-v8a.apk`
- `YTMusicBYD-<version>-android-arm64-v8a.apk`

## Installing on the car

Both apps target Android 10+ arm64. Their package names are the same as the old
WebView apps, so a release APK replaces the old app. If the old app was signed
with a different key, uninstall it once before installing the release APK.

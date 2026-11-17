# AGENTS.md

Guidelines for AI agents working on the BYD Apps project.

## Project Overview

Two standalone Android apps for BYD DiLink 3 head units:

- **YouTube BYD** (`dev.theolm.bydapps.youtube`)
- **YT Music BYD** (`dev.theolm.bydapps.ytmusic`)

Both embed **GeckoView** and share `:browser-core`. They are independent packages
with their own launcher; there is no chooser screen. The old WebView apps and the
old `:core` module were removed.

## Modules

```
browser-core/   Shared GeckoWebAppActivity: GeckoView host, dark scheme,
                MediaSession bridge, back handling, session sharing
apps/youtube/   Start URL, icon, theme, application ID, partner package
apps/ytmusic/   Start URL, icon, theme, application ID, partner package
tools/emu.sh    DiLink 3 emulator workbench
docs/           Emulator, release and session-sharing documentation
```

`GeckoWebAppActivity` owns all browser behavior. An app module should only add
its `startUrl`, `partnerPackages`, resources and manifest entry unless the
feature is app-specific.

## Build Commands

```bash
# Build both debug APKs
./gradlew :apps:youtube:assembleDebug :apps:ytmusic:assembleDebug

# Build a single app
./gradlew :apps:youtube:build
./gradlew :apps:ytmusic:build
./gradlew :browser-core:build

# Lint / checks
./gradlew lint
./gradlew check

# Release (requires a keystore; see docs/release.md)
./gradlew :apps:youtube:assembleRelease :apps:ytmusic:assembleRelease
```

APK outputs:

- `apps/youtube/build/outputs/apk/debug/youtube-debug.apk`
- `apps/ytmusic/build/outputs/apk/debug/ytmusic-debug.apk`

Both APKs are about 111 MB because each bundles the GeckoView engine for
`arm64-v8a`.

## Emulator

```bash
tools/emu.sh setup
tools/emu.sh start
tools/emu.sh install youtube
tools/emu.sh install ytmusic
tools/emu.sh shot build/screens/app.png
```

See [`docs/emulator.md`](docs/emulator.md). The emulator validates layout,
insets, rotation and app-side media keys; it cannot validate the car's CAN/MCU,
audio routing or steering-wheel mapping.

## GeckoView and Kotlin Constraints

- GeckoView is pinned in `gradle/libs.versions.toml`; the apps target Android 10
  (API 29) and only build `arm64-v8a`.
- The page is rendered at 1x with the site's own layout. Do not add CSS/JS
  injection to force zoom or themes unless the task explicitly requires it.
- There is no fullscreen handling by design.
- AGP 9.1's built-in Kotlin compiler (2.2) cannot read the Kotlin 2.4 metadata
  shipped by GeckoView 160. The modules apply the `kotlin-compose` plugin
  2.3.20 as a workaround, and `browser-core` keeps the Compose runtime on the
  classpath. Do not remove either without verifying the build against the
  current GeckoView.
- Use `GeckoRuntime.getDefault(applicationContext)`; `GeckoRuntime.create()`
  allows only one runtime per process and throws when an Activity is recreated.
  The preferred color scheme is applied through `runtime.settings`.
- `android.suppressUnsupportedCompileSdk=37,37.0` in `gradle.properties` is
  expected: GeckoView 160 requires compileSdk 37 while the apps target 34.
- Each app has its own process and `GeckoRuntime`; shared state between YouTube
  and YT Music is not available.

## MediaSession

`browser-core` exposes a `MediaSessionCompat` and forwards play, pause, next,
previous, stop and seek to the page's GeckoView media session. Activate it only
when GeckoView reports an active media session, and keep it active across pause
so play/next still reach the page. A session that is active with no media
steals steering-wheel keys from the other app. Track playback state from
`onPlay`/`onPause`; `MediaSession.isActive` only means the session exists, not
that it is playing.

## Session sharing

The two packages keep separate Gecko profiles, so a Google sign-in in one app
is invisible to the other. `browser-core` bridges the cookie jar between them:

- `SessionBridge` installs the built-in extension in
  `assets/web_extensions/session-bridge/`. This is the only way to read and
  write cookies: GeckoView 160 removed cookies from `StorageController`. Keep
  the `geckoViewAddons` and `nativeMessaging` permissions in its manifest;
  without `geckoViewAddons` GeckoView routes native messaging to the desktop
  path, which fails on Android.
- `SessionSync` exports on `onStop` only when a Google auth cookie (`SID`,
  `__Secure-1PSID`, `__Secure-3PSID`) is present, and imports only when the
  local profile is signed out. Never import over an account that is already
  signed in; it would switch accounts silently.
- The bundle is written to `files/session/session-bundle.json` and shared
  through `SessionProvider` at `content://<applicationId>.sessionsync`,
  protected by the `signature`-level `<applicationId>.permission.SESSION_SYNC`.
  The provider only reads the file and must never start Gecko.
- A new app must list its siblings in `partnerPackages`, declare their
  permissions and add a `<queries>` entry for each sibling package.
- Never log cookie values; only counts and the bundle hash.
- `docs/session-sharing.md` explains the flow and how to test it with a
  synthetic bundle.

## Code Style

- Kotlin official style, 4 spaces, no semicolons, trailing commas in multiline
  lists.
- Classes/objects PascalCase; functions and properties camelCase; constants
  UPPER_SNAKE_CASE; packages lowercase.
- Explicit imports; wildcard imports only for Compose if Compose UI is ever
  introduced.
- Prefer nullable Android types, `?.` and `?:`. Avoid `!!`.
- KDoc for public APIs and non-obvious lifecycle behavior. Explain *why* in
  inline comments.
- Write code, comments, commit messages and docs in English.
- Use `Log.d()`/`Log.e()` instead of `System.out.println()`.

## Android Guidelines

- `minSdk 29`, `targetSdk 34`, `compileSdk 37`, `arm64-v8a`.
- Declare permissions in `AndroidManifest.xml`; `INTERNET` is the only one the
  apps need today.
- Use app resources for labels, colors and themes. Do not hardcode UI strings.
- Keep the dark window background and transparent system bars.
- Do not add Google Play Services or large dependencies; APK size matters.
- Do not reintroduce a start screen or a shared application ID.

## Release

Releases run from `.github/workflows/release.yml` on `vX.Y.Z` tags. The tag must
match `versionName` in `gradle/libs.versions.toml`; `GITHUB_RUN_NUMBER` becomes
`versionCode`. Signing uses `ANDROID_KEYSTORE_BASE64`,
`ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD`.
See [`docs/release.md`](docs/release.md).

Never commit a keystore, password or generated APK (`*.keystore`, `*.apk` and
`build/` are gitignored).

## Testing

There are no unit or instrumentation tests yet. When adding tests, use JUnit 4
and AndroidX Test, name classes `<ClassUnderTest>Test`, and keep tests next to
the existing module layout. Manual verification on `dilink3` is the current
baseline.

## Prohibited

- No Google Play Services.
- No WebView-based browser implementation; GeckoView is the engine.
- No hardcoded UI strings in Kotlin.
- No `!!` without justification.
- No committing secrets, keystores or APKs.

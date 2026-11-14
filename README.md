# BYD Apps for YouTube and YT Music

> **Unofficial project.** Not affiliated with, endorsed by, or sponsored by
> Google LLC, YouTube, or BYD Company Limited. YouTube and YT Music are
> trademarks of Google LLC; BYD is a trademark of BYD Company Limited.

Two standalone Android apps for BYD DiLink 3 head units. Each app embeds
Mozilla's **GeckoView** engine, so the current YouTube and YT Music web apps
render correctly without depending on the car's system WebView.

| App | Package | APK |
| --- | --- | --- |
| **YouTube BYD** | `dev.theolm.bydapps.youtube` | `youtube-release.apk` |
| **YT Music BYD** | `dev.theolm.bydapps.ytmusic` | `ytmusic-release.apk` |

## Why GeckoView

DiLink 3 runs Android 10 with an old system WebView (often Chromium 74). The
current YT Music web app breaks on it: oversized guide icons, a tiny player
bar, 0×0 menus and misplaced sheets. Patching each screen was fragile, so both
apps now bundle GeckoView 160 and render the site's own layout.

Each app keeps:

- the page at **1x**, with the site's natural layout;
- a dark color scheme;
- a native `MediaSession` that forwards steering-wheel next/previous/play/pause
  to the page's media session;
- no custom fullscreen handling.

## Architecture

```
browser-core/       Shared GeckoView host, MediaSession and window setup
apps/youtube/       YouTube BYD: start URL, icon and launcher
apps/ytmusic/       YT Music BYD: start URL, icon and launcher
tools/emu.sh        DiLink 3 emulator workbench
```

`browser-core` contains `GeckoWebAppActivity`. Each app is a tiny `MainActivity`
that only supplies its start URL, so both apps stay in sync.

## Build

```bash
./gradlew :apps:youtube:assembleDebug :apps:ytmusic:assembleDebug
```

The debug APKs are written to:

- `apps/youtube/build/outputs/apk/debug/youtube-debug.apk`
- `apps/ytmusic/build/outputs/apk/debug/ytmusic-debug.apk`

Each APK is about **100-110 MB** because it contains the GeckoView engine and
native libraries for arm64.

## Emulator

`tools/emu.sh` boots a DiLink 3-sized AVD (1920×1080, 240 dpi, landscape,
`[0,84][1920,990]` content area), installs either app, sends media keys and
takes screenshots:

```bash
tools/emu.sh setup
tools/emu.sh start
tools/emu.sh install youtube
tools/emu.sh install ytmusic
tools/emu.sh shot build/screens/app.png
```

See [docs/emulator.md](docs/emulator.md) for the full command list and limits.

## Release

Tags matching `vX.Y.Z` trigger `.github/workflows/release.yml`, which builds
signed APKs and publishes a GitHub release. The keystore and signing secrets
are added in the repository settings; see [docs/release.md](docs/release.md).

## Compatibility

- BYD DiLink 3 head units, Android 10 (API 29) or newer
- arm64-v8a
- No Google Play Services required
- The apps are standalone packages and can be installed side by side

## License

The original source code is MIT — see [LICENSE](LICENSE). Bundled third-party
components keep their own licenses; see
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Trademarks and brand assets
are not covered by the MIT license.

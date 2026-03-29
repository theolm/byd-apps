# YouTube App for BYD

This is the YouTube application module for BYD vehicles. It provides a native YouTube experience by wrapping the mobile website with additional features like fullscreen video, theme synchronization, and download support.

## Features

- 🔐 **Persistent Login** — Stay logged in between sessions (cookie-based)
- 🎬 **Fullscreen Video** — True fullscreen with hardware acceleration
- 🌓 **Theme Sync** — Automatically matches system light/dark mode
- 📥 **Downloads** — Download videos when YouTube allows it
- 🔙 **Smart Navigation** — Back button navigates history, doesn't close app
- 🎵 **Background Playback** — Audio continues when app is backgrounded
- 🚫 **No External Browser** — All links stay within the app

## Configuration

The app is configured via `YouTubeConfig.kt`:

```kotlin
object YouTubeConfig : WebViewConfiguration {
    override val startUrl = "https://www.youtube.com"
    override val enableDownloads = true
    override val enableFullscreen = true
    override val customJavaScript = null
}
```

## Theme

Uses YouTube brand colors:

- **Primary**: YouTube Red (#FF0000)
- **Dark Background**: #0F0F0F
- **Light Background**: #FFFFFF

Theme automatically syncs with system settings.

## Building

```bash
# Debug APK
./gradlew :apps:youtube:assembleDebug

# Release APK
./gradlew :apps:youtube:assembleRelease
```

Output: `apps/youtube/build/outputs/apk/`

## Installation on BYD

1. Build the APK (see above)
2. Copy to USB drive
3. Insert USB into BYD media center
4. Open file manager
5. Tap APK to install
6. Enable "Unknown Sources" if prompted

## Architecture

This app uses the `:core` module for shared functionality:

- `BaseWebView` — Reusable WebView composable
- `WebViewClient` — URL handling and theme injection
- `WebViewChromeClient` — Fullscreen video support
- `WebViewDownloadHandler` — Download management

App-specific code:

- `MainActivity.kt` — Entry point, system UI management
- `YouTubeConfig.kt` — App configuration
- `ui/theme/` — YouTube-specific theming

## Technical Details

### Theme Synchronization

The app injects JavaScript to sync YouTube's theme with Android system theme:

```javascript
// Dark mode:
document.documentElement.setAttribute("dark", "");
document.documentElement.style.colorScheme = "dark";

// Light mode:
document.documentElement.removeAttribute("dark");
document.documentElement.style.colorScheme = "light";
```

### Fullscreen Handling

When YouTube's fullscreen button is clicked:

1. `onShowCustomView()` is called
2. App hides system bars (status bar, navigation)
3. Video view is shown over the WebView
4. Back button exits fullscreen

### Caching

- Static resources cached locally via WebView
- Login persists via cookies
- Videos stream (not cached)

## Known Issues

- Some YouTube Premium features may not work
- Picture-in-picture not yet implemented
- No ad blocking (YouTube's ads are served)

## License

Same as parent project — MIT License

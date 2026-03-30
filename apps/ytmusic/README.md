# YT Music BYD App

YT Music web wrapper for BYD vehicle infotainment systems.

## Overview

This app provides a native Android wrapper for YouTube Music, designed specifically for BYD vehicles that lack Google Play Services. The app loads the web version of YouTube Music (music.youtube.com) in a WebView while providing native features like downloads, fullscreen support, and steering wheel control integration.

## Features

- **Full YT Music Experience**: Access your library, playlists, and recommendations
- **Dark Theme Only**: Optimized for in-car use with pure black background
- **Downloads**: Download music for offline listening
- **Fullscreen Support**: Watch music videos in fullscreen mode
- **Steering Wheel Controls**: Use BYD steering wheel buttons for next/previous/play-pause
- **Background Audio**: Continue playing music when app is in background
- **No Google Play Services Required**: Works on BYD's Android-based infotainment system

## Architecture

The app follows the same architecture as the YouTube BYD app:

- **WebView Wrapper**: Loads music.youtube.com with custom configuration
- **Theme**: Always uses dark theme (black #000000 background)
- **Media Buttons**: JavaScript injection for steering wheel control
- **Configuration**: Centralized via YTMusicConfig object

## Project Structure

```
src/main/java/dev/theolm/bydapps/ytmusic/
├── MainActivity.kt              # Entry point, media button handling
├── config/
│   ├── YTMusicConfig.kt         # WebView configuration
│   └── YTMusicMediaHandler.kt   # Steering wheel media controls
└── ui/theme/
    ├── Color.kt                 # YT Music brand colors (dark only)
    └── Theme.kt                 # Material3 theme (dark only)
```

## Building

Build the debug APK:
```bash
./gradlew :apps:ytmusic:assembleDebug
```

Build the release APK:
```bash
./gradlew :apps:ytmusic:assembleRelease
```

The APK will be located at:
- Debug: `apps/ytmusic/build/outputs/apk/debug/ytmusic-debug.apk`
- Release: `apps/ytmusic/build/outputs/apk/release/ytmusic-release.apk`

## Configuration

The app behavior is configured in `YTMusicConfig.kt`:

```kotlin
object YTMusicConfig : WebViewConfiguration {
    override val startUrl = "https://music.youtube.com"
    override val themeMode = ThemeMode.DARK  // Always dark
    override val enableDownloads = true
    override val enableFullscreen = true
    override val enableSpaNavigationTracking = true
    override val mediaButtonHandler = YTMusicMediaHandler()
}
```

## Media Controls

The BYD steering wheel media buttons are mapped to:
- **Next Track**: Advances to next song
- **Previous Track**: Goes to previous song
- **Play/Pause**: Toggles playback

These are implemented via JavaScript injection that simulates keyboard events and clicks on player controls.

## Theming

YT Music BYD uses a pure dark theme optimized for car displays:
- **Background**: Pure black (#000000)
- **Surface**: Dark gray (#1F1F1F)
- **Primary**: YT Music Red (#FF0000)
- **Text**: White (#FFFFFF)

There is no light mode option - the app is always dark for optimal in-car visibility.

## License

MIT License - Same as the main BYD Apps project.

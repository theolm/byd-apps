# WebView Apps for BYD 🚗

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.7%2B-green.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> A collection of web-based apps for **BYD vehicles** — works without Google Play Services!

## What is this?

This repository contains Android apps designed specifically for **BYD vehicle infotainment systems**. BYD cars run Android but don't include Google Play Services, making it impossible to use official Google apps like YouTube, Netflix, or Spotify.

This project solves that problem by wrapping web-based services in native Android apps using WebView. Each app provides a proper native experience while remaining lightweight and simple to install.

## Available Apps

| App                     | Description                                                        | Status   |
| ----------------------- | ------------------------------------------------------------------ | -------- |
| [YouTube](apps/youtube) | Full YouTube experience with downloads, fullscreen, and theme sync | ✅ Ready |

## Why This Project Exists

**BYD** (Build Your Dreams) is one of the world's largest electric vehicle manufacturers. Their vehicles run Android-based infotainment systems, but **they don't include Google Play Services** due to regional restrictions.

This means:

- ❌ No Google Play Store
- ❌ No official YouTube/Netflix/Spotify apps
- ❌ Complex workarounds needed to install apps

### The Solution

Just sideload the APK files directly to your BYD media center. No rooting, no microG, no complex setup. Each app is:

- **Lightweight** — Just wraps the web service with native features
- **Self-contained** — No dependencies on Google services
- **Easy to install** — Simple APK sideloading

**Note:** While designed for BYD vehicles, these apps work on any Android device running Android 10+.

## Architecture

This project uses a modular architecture with shared core components:

```
YouTubeBYD/
├── core/                    # Shared library (WebView wrapper, downloads, etc.)
├── apps/
│   └── youtube/            # Individual app modules
│       ├── src/            # App-specific code and resources
│       └── build.gradle    # App build configuration
└── README.md               # This file
```

### Core Module

The `:core` module provides reusable components for all apps:

- **BaseWebView** — Configurable WebView composable with fullscreen support
- **WebViewClient** — URL handling and JavaScript injection
- **WebViewChromeClient** — Fullscreen video handling
- **DownloadHandler** — File download management
- **ThemeProvider** — System theme synchronization

Each app module:

- Implements its own configuration (URL, features, theme)
- Has its own app icon and branding
- Can be built independently
- Produces a separate APK

## Installation

### Build from Source

1. **Clone the repo**

   ```bash
   git clone https://github.com/theolm/youtube-byd.git
   cd youtube-byd
   ```

2. **Open in Android Studio**

   - Open the project folder
   - Let Gradle sync
   - Select the app module you want to build (e.g., `:apps:youtube`)

3. **Build the APK**

   ```bash
   # Build specific app
   ./gradlew :apps:youtube:assembleDebug

   # Or build all apps
   ./gradlew assembleDebug
   ```

4. **Install on your BYD**
   - Enable "Unknown Sources" in settings
   - Copy APK to USB drive
   - Install via file manager

### Requirements

- Android 10 (API 29) or higher
- Internet connection

## Creating New Apps

Want to add a new app (e.g., Netflix, Spotify)? Follow these steps:

1. **Create app folder**

   ```bash
   mkdir apps/newapp
   ```

2. **Copy youtube app as template**

   ```bash
   cp -r apps/youtube/* apps/newapp/
   ```

3. **Update configuration**

   - Edit `apps/newapp/build.gradle.kts`: change `applicationId` and `namespace`
   - Edit `apps/newapp/src/main/java/.../config/NewAppConfig.kt`: set URL and features
   - Create your theme in `apps/newapp/src/main/java/.../ui/theme/`
   - Replace icons in `apps/newapp/src/main/res/mipmap-*/`

4. **Add to settings.gradle.kts**

   ```kotlin
   include(":apps:newapp")
   ```

5. **Build and test**
   ```bash
   ./gradlew :apps:newapp:assembleDebug
   ```

See [apps/youtube](apps/youtube) for a complete working example.

## Technical Highlights

- **Jetpack Compose** — Modern declarative UI
- **Modular Architecture** — Shared core, independent apps
- **JavaScript Injection** — Syncs web app themes with Android system theme
- **Fullscreen Support** — True fullscreen video with hardware acceleration
- **State Management** — Proper Compose state handling
- **Clean Code** — Follows Android best practices

## Roadmap

### Core Features

- [x] Base WebView wrapper
- [x] Fullscreen video support
- [x] Theme synchronization
- [x] Download handling
- [ ] Offline support
- [ ] Ad blocking (optional)

### New Apps

- [x] YouTube
- [ ] Netflix
- [ ] Spotify
- [ ] Disney+
- [ ] Amazon Prime Video

## Contributing

PRs welcome! This is a community project for BYD owners.

1. Fork it
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

MIT License — do whatever you want, just don't blame me if something breaks.

```
MIT License

Copyright (c) 2024 Theo

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

**Not affiliated with Google, YouTube, BYD, or any streaming services.** These are unofficial wrappers for personal use. Service terms still apply.

Built with ☕ and Kotlin Compose for BYD owners who just want to enjoy their favorite content in their cars.

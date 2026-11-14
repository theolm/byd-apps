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

## Testing without the car

`tools/emu.sh` boots a DiLink 3-sized AVD (1920x1080, 240 dpi, landscape,
`[0,84][1920,990]` content area) and installs the apps into it. It also sends
media keys through `MediaSession` and exposes the WebView to Chrome DevTools.
See [docs/emulator.md](docs/emulator.md).

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

### New Apps

- [x] YouTube
- [ ] Netflix
- [ ] Spotify
- [ ] Disney+
- [ ] Amazon Prime Video

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

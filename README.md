# YouTube for BYD 🚗

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.7%2B-green.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> YouTube client designed specifically for **BYD vehicles** — works without Google Play Services!

## What is this?

This is a lightweight YouTube client built for **BYD vehicle infotainment systems**. BYD cars run Android but don't include Google Play Services, which means no YouTube app and no easy way to install it.

This app wraps YouTube's mobile website in a native Android container using WebView, giving you a proper app experience without needing Google services.

**Why use this instead of the browser?**

- 🔐 **Stay logged in** — No more signing in every time
- 🎬 **True fullscreen** — Videos expand to fill your entire screen
- 🌓 **Follows your theme** — Automatically switches between light and dark mode with your system
- 📥 **Download support** — Grab videos when YouTube allows it
- 🔙 **Smart navigation** — Back button goes back in history, doesn't close the app
- 🎵 **Background audio** — Keep listening when you switch apps

## Why This App Exists

**BYD** (Build Your Dreams) is one of the world's largest electric vehicle manufacturers. Their vehicles run Android-based infotainment systems, but **they don't include Google Play Services** due to regional restrictions.

This means:

- ❌ No Google Play Store
- ❌ No official YouTube app
- ❌ No easy way to install Google apps

### The Solution

This app provides a **simple, native YouTube experience** for your BYD vehicle's media center. Just sideload the APK and you have full YouTube functionality without:

- Rooting your car's system
- Installing complex Google service workarounds (microG)
- Dealing with compatibility issues

**Note:** While designed for BYD vehicles, this app works on any Android device running Android 10+.

## Features

| Feature             | Status                                        |
| ------------------- | --------------------------------------------- |
| Persistent Login    | ✅ Cookie-based, survives app restarts        |
| Fullscreen Video    | ✅ True fullscreen with hardware acceleration |
| System Theme Sync   | ✅ Light/Dark mode matches your Android theme |
| Downloads           | ✅ Uses Android's DownloadManager             |
| Back Navigation     | ✅ Smart back button handling                 |
| Background Playback | ✅ Audio continues in background              |
| No External Browser | ✅ All links stay in-app                      |

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
   - Select your device/emulator

3. **Build and Run**
   - Click the ▶️ Run button (or `Shift+F10`)
   - APK will be generated at `app/build/outputs/apk/debug/`

### Requirements

- Android 10 (API 29) or higher
- Internet connection (obviously)

## Technical Highlights

This isn't just a basic WebView slapped together. Here's what makes it tick:

- **Jetpack Compose** — Modern declarative UI
- **Custom WebChromeClient** — Handles fullscreen video properly
- **JavaScript Injection** — Syncs YouTube's theme with your Android system theme
- **State Management** — Proper Compose state handling with `rememberUpdatedState` and `derivedStateOf`
- **Clean Architecture** — Separated into focused modules (Config, Clients, Handlers)
- **Edge-to-Edge** — Transparent status/navigation bars for immersive experience

## Project Structure

```
app/src/main/java/dev/theolm/youtubebyd/
├── MainActivity.kt              # Entry point, sets up edge-to-edge
├── YouTubeWebView.kt            # Main composable with state management
├── YouTubeConstants.kt          # URLs, user agents, JS snippets
├── WebViewConfig.kt             # WebView settings configuration
├── YouTubeWebChromeClient.kt    # Fullscreen video handling
├── YouTubeWebViewClient.kt      # URL loading and error handling
├── YouTubeDownloadHandler.kt    # File download management
└── ui/theme/                    # YouTube-inspired color scheme
```

## How It Works

### Theme Synchronization

The app injects JavaScript into YouTube to toggle dark mode based on your system preference:

```javascript
// When system is in dark mode:
document.documentElement.setAttribute("dark", "");
document.documentElement.style.colorScheme = "dark";

// When system is in light mode:
document.documentElement.removeAttribute("dark");
document.documentElement.style.colorScheme = "light";
```

This happens automatically whenever you change your system's theme.

### Fullscreen Handling

YouTube's fullscreen button triggers `WebChromeClient.onShowCustomView()`, which we intercept to show the video view over the WebView. The back button smartly exits fullscreen first, then navigates back, then exits the app.

### Caching

WebView uses `LOAD_DEFAULT` cache mode, meaning:

- Static resources (JS, CSS, images) are cached locally
- Only new/changed content is downloaded
- Your login persists via cookies
- **Note:** Videos are streamed, not cached

## Roadmap

- [ ] Picture-in-Picture mode
- [ ] Custom playback speed controls
- [ ] SponsorBlock integration
- [ ] Optional ad blocking
- [ ] Offline playlist support

## Contributing

PRs welcome! This is a small project, so keep it simple:

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

**Not affiliated with Google or YouTube.** This is an unofficial wrapper for personal use. YouTube's terms of service still apply.

Built with ☕ and Kotlin Compose specifically for BYD car owners who just want to watch YouTube without the Google Play Services hassle.

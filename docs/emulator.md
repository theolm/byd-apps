# Emulator workbench

`tools/emu.sh` runs the apps on an AVD that matches the BYD Dolphin Plus head
unit closely enough to iterate on layout, insets, rotation and media keys
without powering the car on.

It follows the geometry used by
[wheregoes/byd-apps](https://github.com/wheregoes/byd-apps): **1920x1080 at
240 dpi, landscape**, with the car's stable frame `[0,84][1920,990]`
reproduced by padding the AOSP bars with `wm overscan`.

## Requirements

- Android SDK with `platform-tools`, `emulator` and `cmdline-tools`
- JDK 21 (Gradle downloads it through the Foojay resolver if missing)

The script looks for the SDK at `ANDROID_SDK_ROOT`, `ANDROID_HOME`,
`~/Library/Android/sdk` or `~/Android/Sdk`, in that order.

## First run

```bash
tools/emu.sh setup     # downloads the API 29 arm64/x86_64 image, creates "dilink3"
tools/emu.sh start     # boots the AVD and applies size/density/overscan
tools/emu.sh install youtube
tools/emu.sh install ytmusic
```

On Apple Silicon the script picks `system-images;android-29;default;arm64-v8a`.
The app embeds GeckoView, so the system WebView version does not affect the
rendered UI; the image is only there to reproduce the head unit geometry and
Android 10 behavior.

`install` pins `ANDROID_SERIAL=emulator-5556`, so it can never install on the
car while the car is connected over Wi-Fi, and it does not collide with a
Pixel AVD already running on 5554. Use `EMU_PORT`/`EMU_SERIAL` to change it.

## Commands

```bash
tools/emu.sh shot build/screens/home.png
tools/emu.sh key next           # KEYCODE_MEDIA_NEXT through input
tools/emu.sh key --session next # media dispatch to the active session
tools/emu.sh key playpause
tools/emu.sh rotate portrait
tools/emu.sh info               # wm size/density/overscan and stable insets
tools/emu.sh stop
```

## Media keys

`key` sends `input keyevent` by default, which goes through the same input
pipeline as the steering wheel. `--session` uses `media dispatch`, which calls
the active MediaSession directly. Both paths should reach the
`MediaSessionCompat` in `browser-core`.

## Limits

The emulator proves layout, insets, rotation and the app-side media session.
It does not reproduce the car's CAN/MCU, audio routing, exact GeckoView
version or the actual steering-wheel key mapping inside DiLink. Google login
inside GeckoView may also be blocked.

Both apps request a dark color scheme and open in the sites' dark themes. A
signed-in Google account can still override the site theme in its own settings.

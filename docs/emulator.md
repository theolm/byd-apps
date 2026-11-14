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
- Node 22+ only for `eval` and `zoom`

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
That image ships the same generation of WebView as an untouched Android 10
head unit (WebView 74). Override it if the car's `dumpsys webviewupdate` shows
a newer version:

```bash
EMU_IMAGE="system-images;android-34;google_apis;arm64-v8a" \
EMU_RECREATE=1 tools/emu.sh setup
```

`install` pins `ANDROID_SERIAL=emulator-5556`, so it can never install on the
car while the car is connected over Wi-Fi, and it does not collide with a
Pixel AVD already running on 5554. Use `EMU_PORT`/`EMU_SERIAL` to change it.

## Commands

```bash
tools/emu.sh shot build/screens/home.png
tools/emu.sh key next          # KEYCODE_MEDIA_NEXT through input
tools/emu.sh key --session next # media dispatch to the active session
tools/emu.sh key playpause
tools/emu.sh rotate portrait
tools/emu.sh info              # wm size/density/overscan and stable insets
tools/emu.sh stop
```

### WebView DevTools

The apps enable `WebView.setWebContentsDebuggingEnabled(true)` in debuggable
builds. Forward the socket and open `chrome://inspect` in Chrome:

```bash
tools/emu.sh webview dev.theolm.bydapps.youtube
tools/emu.sh eval "document.title" dev.theolm.bydapps.youtube
tools/emu.sh zoom 1.25 build/screens/youtube-125.png
```

This also works on the car with a debug APK (`adb connect
192.168.10.10:5555`), which is useful for checking a layout change in place.

### Media keys

`key` sends `input keyevent` by default, which goes through the same input
pipeline as the steering wheel. `--session` uses `media dispatch`, which calls
the active MediaSession directly. DiLink routes the wheel through the active
media session; with no `MediaSession` in the app, neither command affects
YouTube. `cmd media_session` is not implemented on the API 29 image, so the
script uses `media dispatch` instead.

The emulator can prove that the session callback and the JavaScript it runs
work. It cannot prove how the car's CAN/framework maps a physical button to a
media key; that is the one part that still needs a capture from the car.

## Limits

| Area | Emulator | Car only |
| --- | --- | --- |
| Layout, density, bars, rotation, fullscreen | yes | |
| Standard media keys + MediaSession | yes | |
| Exact WebView version | only if the image matches | |
| Steering-wheel routing inside DiLink | no | yes |
| Audio, MCU, GPU performance | no | yes |
| Google login inside WebView | may be blocked | yes |

The emulator window may be scaled down on a laptop screen. Judge the
1920x1080 PNGs, not the window size.

If the emulator shows a black screen or crashes with the host GPU, retry with
`EMU_GPU=swiftshader_indirect tools/emu.sh start`. Boot logs go to
`/tmp/dilink3-emu.log`.

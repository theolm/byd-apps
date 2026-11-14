#!/usr/bin/env bash
# DiLink 3 emulator workbench for the BYD WebView apps.
#
# Geometry follows wheregoes/byd-apps: 1920x1080 at 240 dpi, landscape, with
# the car's stable frame [0,84][1920,990] reproduced by padding the AOSP
# status/navigation bars with `wm overscan`. This proves layout, insets,
# rotation and standard media-key handling. It proves NOTHING about the car's
# WebView version, CAN/MCU behaviour or how DiLink routes the steering wheel
# keys internally.
#
#   tools/emu.sh setup                     # one-time: API 29 image + AVD
#   tools/emu.sh start                     # boot (EMU_HEADLESS=1 for no window)
#   tools/emu.sh install youtube           # Gradle build, install, launch
#   tools/emu.sh install ytmusic
#   tools/emu.sh shot build/screens/app.png
#   tools/emu.sh key next|prev|play|pause|playpause
#   tools/emu.sh rotate landscape|portrait
#   tools/emu.sh webview [package]         # Chrome DevTools over adb
#   tools/emu.sh eval '<js>' [package]     # evaluate JS in the WebView
#   tools/emu.sh zoom 1.25 [file]          # CSS zoom + screenshot
#   tools/emu.sh info                      # size, density, overscan, insets
#   tools/emu.sh stop
#
# Env overrides:
#   EMU_IMAGE     system image (default: API 29 default, host arch)
#   EMU_AVD       AVD name (default: dilink3)
#   EMU_PORT      emulator console port (default: 5556)
#   EMU_SERIAL    emulator serial (default: emulator-$EMU_PORT)
#   EMU_HEADLESS  1 = start without a window
#   EMU_AUDIO     1 = enable audio (off by default, like the reference script)
#   EMU_GPU       emulator GPU mode (default: host)
#   EMU_RECREATE  1 = recreate the AVD during setup
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
if [ ! -d "$SDK" ]; then
    for candidate in "$HOME/Android/Sdk" "$HOME/Library/Android/sdk"; do
        if [ -d "$candidate" ]; then
            SDK="$candidate"
            break
        fi
    done
fi

ADB="$SDK/platform-tools/adb"
EMULATOR="$SDK/emulator/emulator"
SDKMANAGER="$SDK/cmdline-tools/latest/bin/sdkmanager"
AVDMANAGER="$SDK/cmdline-tools/latest/bin/avdmanager"

AVD="${EMU_AVD:-dilink3}"
case "$(uname -m)" in
    arm64|aarch64) DEFAULT_IMAGE="system-images;android-29;default;arm64-v8a" ;;
    x86_64|amd64)  DEFAULT_IMAGE="system-images;android-29;default;x86_64" ;;
    *) echo "Unsupported host architecture: $(uname -m)" >&2; exit 1 ;;
esac
IMAGE="${EMU_IMAGE:-$DEFAULT_IMAGE}"

INI="$HOME/.android/avd/$AVD.avd/config.ini"
# Pin the emulator: an untargeted adb command would hit the car when it is
# connected over Wi-Fi at the same time. 5556 by default because 5554/5558 are
# frequently taken by an already-running Pixel AVD.
EMU_PORT="${EMU_PORT:-5556}"
export ANDROID_SERIAL="${EMU_SERIAL:-emulator-$EMU_PORT}"

SCREEN_DIR="$REPO_ROOT/build/screens"

die() { echo "ERROR: $*" >&2; exit 1; }

need_sdk() {
    [ -x "$ADB" ] || die "adb not found at $ADB (set ANDROID_SDK_ROOT)"
    [ -x "$EMULATOR" ] || die "emulator not found at $EMULATOR"
    [ -f "$INI" ] || die "AVD $AVD not found; run: tools/emu.sh setup"
}

need_node() {
    command -v node >/dev/null 2>&1 || die "node is required for eval/zoom"
}

usage() {
    cat <<EOF
usage: tools/emu.sh <command>

  setup                 install platform-tools, emulator, $IMAGE and
                        create the $AVD AVD with DiLink 3 geometry
  start                 boot the AVD and apply geometry
                        (EMU_HEADLESS=1 for no window, EMU_AUDIO=1 for audio)
  stop                  kill the running AVD
  install <app>         Gradle installDebug + launch
                        (youtube|ytmusic|gecko-youtube|gecko-ytmusic)
  shot [file]           screencap to file (default: build/screens/emu-<time>.png)
  key [--session] <action>
                        send next|prev|play|pause|playpause through input
                        key events (--session uses `media dispatch`)
  rotate <orientation>  landscape|portrait
  webview [package]     forward the WebView devtools socket to :9222
  eval <js> [package]   evaluate JavaScript in the foreground WebView
  zoom <scale> [file]   set CSS zoom on the top document and screenshot
  info                  print size, density, overscan and window insets
  help                  this text
EOF
}

cfg() {
    local key="$1" value="$2" file="$3" tmp
    tmp="${file}.tmp"
    grep -v "^${key}=" "$file" > "$tmp" || true
    printf '%s=%s\n' "$key" "$value" >> "$tmp"
    mv "$tmp" "$file"
}

apply_geometry() {
    "$ADB" shell wm size 1920x1080 >/dev/null 2>&1 || true
    "$ADB" shell wm density 240 >/dev/null 2>&1 || true
    # Emulator bars at 240 dpi: status 36 px, nav 72 px. The car has 84 px and
    # 90 px, so pad them by 48 and 18. Resulting content: [0,84][1920,990].
    "$ADB" shell wm overscan 0,48,0,18 >/dev/null 2>&1 || \
        echo "warning: wm overscan failed" >&2
    "$ADB" shell settings put global window_animation_scale 0 >/dev/null 2>&1 || true
    "$ADB" shell settings put global transition_animation_scale 0 >/dev/null 2>&1 || true
    "$ADB" shell settings put global animator_duration_scale 0 >/dev/null 2>&1 || true
    "$ADB" shell settings put system screen_off_timeout 2147483647 >/dev/null 2>&1 || true
}

cmd_setup() {
    [ -x "$SDKMANAGER" ] || die "sdkmanager not found at $SDKMANAGER"
    [ -x "$AVDMANAGER" ] || die "avdmanager not found at $AVDMANAGER"
    echo "=== SDK: $SDK ==="
    echo "=== Image: $IMAGE ==="
    # sdkmanager may ask for licences; accept them non-interactively. `yes`
    # dies of SIGPIPE when sdkmanager exits, so keep pipefail from aborting.
    (yes || true) | "$SDKMANAGER" --sdk_root="$SDK" --licenses >/dev/null
    "$SDKMANAGER" --sdk_root="$SDK" "platform-tools" "emulator" "$IMAGE"

    if [ ! -d "$HOME/.android/avd/$AVD.avd" ] || [ "${EMU_RECREATE:-0}" = 1 ]; then
        echo "=== Creating AVD $AVD ==="
        rm -rf "$HOME/.android/avd/$AVD.avd" "$HOME/.android/avd/$AVD.ini"
        echo no | "$AVDMANAGER" create avd -n "$AVD" -k "$IMAGE"
    fi

    [ -f "$INI" ] || die "AVD config not found at $INI"
    echo "=== Applying DiLink 3 geometry ==="
    cfg hw.lcd.width 1920 "$INI"
    cfg hw.lcd.height 1080 "$INI"
    cfg hw.lcd.density 240 "$INI"
    cfg skin.name 1920x1080 "$INI"
    cfg skin.path _no_skin "$INI"
    cfg hw.initialOrientation landscape "$INI"
    cfg hw.keyboard yes "$INI"
    cfg hw.mainKeys no "$INI"
    cfg hw.gpu.enabled yes "$INI"
    cfg hw.gpu.mode host "$INI"
    cfg hw.ramSize 2048 "$INI"
    cfg hw.cpu.ncore 4 "$INI"
    cfg vm.heapSize 256 "$INI"
    cfg disk.dataPartition.size 2G "$INI"
    cfg showDeviceFrame no "$INI"

    echo "=== AVDs ==="
    "$EMULATOR" -list-avds
}

cmd_start() {
    need_sdk
    if "$ADB" get-state >/dev/null 2>&1; then
        local running_avd
        running_avd="$("$ADB" emu avd name 2>/dev/null | head -1 | tr -d '\r')"
        if [ "$running_avd" != "$AVD" ]; then
            die "$ANDROID_SERIAL is running '$running_avd', not '$AVD'; set EMU_PORT/EMU_SERIAL"
        fi
        echo "$ANDROID_SERIAL already running"
    else
        echo "=== Booting $AVD on $ANDROID_SERIAL ==="
        local args=(-avd "$AVD" -port "$EMU_PORT" -no-boot-anim -gpu "${EMU_GPU:-host}")
        if [ "${EMU_AUDIO:-0}" != 1 ]; then
            args+=(-no-audio)
        fi
        if [ "${EMU_HEADLESS:-0}" = 1 ]; then
            args+=(-no-window)
        fi
        "$EMULATOR" "${args[@]}" >/tmp/"$AVD"-emu.log 2>&1 &
        "$ADB" wait-for-device
    fi

    local i
    for i in $(seq 180); do
        if [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; then
            echo "=== Booted ==="
            "$ADB" shell wm size | tr -d '\r'
            "$ADB" shell wm density | tr -d '\r'
            apply_geometry
            return 0
        fi
        sleep 1
    done
    tail -n 40 /tmp/"$AVD"-emu.log >&2 || true
    die "boot timed out after 180s (see /tmp/$AVD-emu.log)"
}

cmd_stop() {
    "$ADB" emu kill >/dev/null 2>&1 || true
    echo "stopped"
}

cmd_install() {
    need_sdk
    local app="${1:-}" module pkg
    case "$app" in
        youtube) module=":apps:youtube"; pkg="dev.theolm.bydapps.youtube" ;;
        ytmusic) module=":apps:ytmusic"; pkg="dev.theolm.bydapps.ytmusic" ;;
        gecko-youtube) module=":apps:gecko-youtube"; pkg="dev.theolm.bydapps.gecko.youtube" ;;
        gecko-ytmusic) module=":apps:gecko-ytmusic"; pkg="dev.theolm.bydapps.gecko.ytmusic" ;;
        *) die "install needs youtube, ytmusic, gecko-youtube or gecko-ytmusic" ;;
    esac
    "$ADB" get-state >/dev/null 2>&1 || die "emulator not running; run: tools/emu.sh start"
    echo "=== Building and installing $app ==="
    (cd "$REPO_ROOT" && ANDROID_HOME="$SDK" ANDROID_SDK_ROOT="$SDK" \
        ./gradlew "$module:installDebug")
    echo "=== Launching $pkg ==="
    "$ADB" shell am force-stop "$pkg" >/dev/null 2>&1 || true
    "$ADB" shell am start -n "$pkg/.MainActivity" >/dev/null
}

cmd_shot() {
    need_sdk
    local out="${1:-$SCREEN_DIR/emu-$(date +%Y%m%d-%H%M%S).png}"
    mkdir -p "$(dirname "$out")"
    "$ADB" exec-out screencap -p > "$out"
    echo "$out"
}

cmd_key() {
    need_sdk
    local mode="input"
    if [ "${1:-}" = "--session" ]; then
        mode="session"
        shift
    fi
    local what="${1:-}" code action
    case "$what" in
        next)      code=87;  action=next ;;
        prev)      code=88;  action=previous ;;
        play)      code=126; action=play ;;
        pause)     code=127; action=pause ;;
        playpause) code=85;  action=play-pause ;;
        *) die "key needs next|prev|play|pause|playpause" ;;
    esac
    if [ "$mode" = session ]; then
        "$ADB" shell media dispatch "$action" >/dev/null
    else
        "$ADB" shell input keyevent "$code" >/dev/null
    fi
    echo "sent $what ($mode)"
}

cmd_rotate() {
    need_sdk
    local mode="${1:-landscape}"
    "$ADB" shell settings put system accelerometer_rotation 0 >/dev/null
    case "$mode" in
        landscape) "$ADB" shell settings put system user_rotation 0 >/dev/null ;;
        portrait)  "$ADB" shell settings put system user_rotation 1 >/dev/null ;;
        *) die "rotate needs landscape or portrait" ;;
    esac
    echo "rotation: $mode"
}

foreground_pkg() {
    "$ADB" shell dumpsys window 2>/dev/null \
        | sed -n 's/.*mCurrentFocus=Window{[^ ]* [^ ]* \([^/]*\)\/.*/\1/p' \
        | head -1 | tr -d '\r'
}

forward_webview() {
    local pkg="${1:-}" pid
    [ -n "$pkg" ] || pkg="$(foreground_pkg)"
    [ -n "$pkg" ] || die "could not detect the foreground package"
    pid="$("$ADB" shell pidof "$pkg" | tr -d '\r')"
    [ -n "$pid" ] || die "no running process for $pkg"
    # A stale forward to the previous app would silently keep its WebView.
    "$ADB" forward --remove tcp:9222 >/dev/null 2>&1 || true
    "$ADB" forward tcp:9222 "localabstract:webview_devtools_remote_$pid" >/dev/null
    echo "$pkg"
}

cmd_webview() {
    need_sdk
    local pkg
    pkg="$(forward_webview "${1:-}")"
    echo "Forwarded WebView of $pkg to localhost:9222"
    echo "Open chrome://inspect in Chrome, or http://localhost:9222/json"
    curl -fsS --max-time 2 http://localhost:9222/json/version 2>/dev/null || true
}

cmd_eval() {
    need_sdk
    need_node
    local expr="${1:-}"
    [ -n "$expr" ] || die "eval needs a JavaScript expression"
    forward_webview "${2:-}" >/dev/null
    node "$REPO_ROOT/tools/webview-eval.mjs" "$expr"
}

cmd_zoom() {
    need_sdk
    need_node
    local scale="${1:-1.25}" out="${2:-$SCREEN_DIR/zoom-$scale.png}"
    forward_webview "${3:-}" >/dev/null
    node "$REPO_ROOT/tools/webview-eval.mjs" \
        "document.documentElement.style.zoom='${scale}'"
    sleep 1
    cmd_shot "$out"
}

cmd_info() {
    need_sdk
    echo "=== size ==="
    "$ADB" shell wm size | tr -d '\r'
    echo "=== density ==="
    "$ADB" shell wm density | tr -d '\r'
    echo "=== overscan ==="
    "$ADB" shell wm overscan 2>/dev/null | tr -d '\r' || true
    echo "=== window ==="
    "$ADB" shell dumpsys window displays 2>/dev/null \
        | grep -E 'init=|cur=|app=|mStable' || true
}

case "${1:-}" in
    setup) cmd_setup ;;
    start) cmd_start ;;
    stop) cmd_stop ;;
    install) shift; cmd_install "$@" ;;
    shot) shift; cmd_shot "$@" ;;
    key) shift; cmd_key "$@" ;;
    rotate) shift; cmd_rotate "$@" ;;
    webview) shift; cmd_webview "$@" ;;
    eval) shift; cmd_eval "$@" ;;
    zoom) shift; cmd_zoom "$@" ;;
    info) cmd_info ;;
    help|-h|--help) usage ;;
    *) usage >&2; exit 1 ;;
esac

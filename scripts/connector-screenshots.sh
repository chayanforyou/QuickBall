#!/usr/bin/env bash
#
# Screenshots the expanded radial menu for each connector style on a connected device.
#
# For every style it writes the setting, restarts the accessibility service, opens the
# menu and saves a screenshot. Your own settings are backed up first and restored at the
# end, even if the script fails or is stopped with Ctrl+C.
#
# Requirements:
#   - One device or emulator connected over adb
#   - A debug build installed (run-as needs a debuggable app), for example:
#       ./gradlew :app:installFossDebug
#   - QuickBall's accessibility service enabled once on the device
#   - The ball parked on the right edge (stashed as the edge handle, or visible)
#
# Usage:
#   scripts/connector-screenshots.sh                  # all styles
#   scripts/connector-screenshots.sh WAVE CURVE       # only these styles
#
# Options (environment variables):
#   TAP_X, TAP_Y      Where to tap to open the menu, in device pixels. Default: the right
#                     edge at 46% of the screen height. Set these if your ball is elsewhere.
#   TEST_COLOR        Connector color used for the shots, as a signed ARGB int.
#                     Default -1710619 (#E5E5E5), visible on dark wallpapers.
#   OUT_DIR           Where screenshots go. Default: build/connector-screenshots
#   CROP              Crop to the menu area with macOS sips: 1 (default) or 0.

set -euo pipefail

PKG="io.github.chayanforyou.quickball"
SERVICE="$PKG/$PKG.core.QuickBallService"
PREFS="shared_prefs/quick_ball_prefs.xml"

ALL_STYLES=(NONE LINE DOTTED ARC LINE_RING DOT_RING CURVE FADE DASH TAPER WAVE BUBBLE ZIGZAG DOUBLE GLOW)
if [ $# -gt 0 ]; then STYLES=("$@"); else STYLES=("${ALL_STYLES[@]}"); fi

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="${OUT_DIR:-$PROJECT_DIR/build/connector-screenshots}"
TEST_COLOR="${TEST_COLOR:--1710619}"
CROP="${CROP:-1}"
BACKUP="$OUT_DIR/quick_ball_prefs.backup.xml"

mkdir -p "$OUT_DIR"

# ---- checks -------------------------------------------------------------------------

if [ "$(adb devices | grep -c -w device)" -ne 1 ]; then
    echo "Connect exactly one device or emulator (adb devices)." >&2
    exit 1
fi

if ! adb shell run-as "$PKG" ls "$PREFS" >/dev/null 2>&1; then
    echo "Cannot read app settings. Install a debug build and open the app once." >&2
    exit 1
fi

read -r WIDTH HEIGHT < <(adb shell wm size | sed -n 's/.*: \([0-9]*\)x\([0-9]*\).*/\1 \2/p' | tail -1)
TAP_X="${TAP_X:-$((WIDTH - 15))}"
TAP_Y="${TAP_Y:-$((HEIGHT * 46 / 100))}"

# ---- helpers ------------------------------------------------------------------------

restart_service() {
    adb shell am force-stop "$PKG"
    adb shell settings put secure enabled_accessibility_services null
    sleep 1
    adb shell settings put secure enabled_accessibility_services "$SERVICE"
    adb shell settings put secure accessibility_enabled 1
    sleep 3
}

write_prefs() {
    # Pipe through the device, since run-as cannot read files pushed by adb directly
    adb push "$1" /data/local/tmp/qb_prefs.xml >/dev/null
    adb shell "cat /data/local/tmp/qb_prefs.xml | run-as $PKG sh -c 'cat > $PREFS'"
    adb shell rm -f /data/local/tmp/qb_prefs.xml
}

# 1 while the full-screen menu window is shown, otherwise 0
menu_is_open() {
    adb shell dumpsys window windows \
        | awk -v pkg="$PKG" '/Window #/{inwin=index($0, pkg) > 0} inwin && /fillxfill/{c++} END{print (c > 0) ? 1 : 0}'
}

# Tap the ball or edge handle until the menu opens; the service may still be starting
open_menu() {
    for attempt in 1 2 3 4 5; do
        # A late-opening menu would be closed again by a second tap, so check first
        [ "$(menu_is_open)" = "1" ] && sleep 0.5 && return 0
        adb shell input tap "$TAP_X" "$TAP_Y"
        sleep 1.5
        [ "$(menu_is_open)" = "1" ] && return 0
        sleep 1
    done
    echo "  Menu did not open. Set TAP_X and TAP_Y to the ball position." >&2
    return 1
}

restore() {
    echo "Restoring your settings..."
    adb shell am force-stop "$PKG" || true
    write_prefs "$BACKUP" || true
    restart_service || true
    adb shell input keyevent KEYCODE_HOME >/dev/null 2>&1 || true
}

# ---- run ----------------------------------------------------------------------------

adb shell run-as "$PKG" cat "$PREFS" > "$BACKUP"
trap restore EXIT

for STYLE in "${STYLES[@]}"; do
    echo "Capturing $STYLE"

    # Settings with this style and the test color; other values stay as they were
    TEMP="$OUT_DIR/prefs_$STYLE.xml"
    STYLE="$STYLE" COLOR="$TEST_COLOR" perl -ne '
        next if /menu_connector_(style|color)/;
        s{</map>}{    <string name="menu_connector_style">$ENV{STYLE}</string>\n    <int name="menu_connector_color" value="$ENV{COLOR}" />\n</map>};
        print' "$BACKUP" > "$TEMP"

    # Force-stop also closes a menu left open by the previous style
    adb shell am force-stop "$PKG"
    write_prefs "$TEMP"
    rm -f "$TEMP"
    restart_service

    adb shell input keyevent KEYCODE_HOME
    sleep 1
    open_menu || continue

    SHOT="$OUT_DIR/$STYLE.png"
    adb exec-out screencap -p > "$SHOT"

    # Keep the right part of the screen around the ball
    if [ "$CROP" = "1" ] && command -v sips >/dev/null; then
        CROP_W=$((WIDTH * 61 / 100))
        CROP_H=$((HEIGHT * 44 / 100))
        OFFSET_Y=$((TAP_Y - CROP_H / 2))
        [ "$OFFSET_Y" -lt 0 ] && OFFSET_Y=0
        sips --cropOffset "$OFFSET_Y" "$((WIDTH - CROP_W))" -c "$CROP_H" "$CROP_W" "$SHOT" >/dev/null
    fi
done

echo "Screenshots saved to $OUT_DIR"

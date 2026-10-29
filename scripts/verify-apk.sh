#!/usr/bin/env bash
set -euo pipefail
APK_PATH="${1:-app/build/outputs/apk/debug/app-debug.apk}"
: "${ANDROID_HOME:?Укажите ANDROID_HOME}"
test -s "$APK_PATH"
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose "$APK_PATH"
"$ANDROID_HOME/build-tools/35.0.0/aapt" dump badging "$APK_PATH"
sha256sum "$APK_PATH"

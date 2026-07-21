#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

echo "Building admin APK (clean)..."
bash gradlew :admin:clean :admin:assembleDebug

APK="admin/build/outputs/apk/debug/admin-debug.apk"
if [[ ! -f "$APK" ]]; then
  echo "APK not found: $APK" >&2
  exit 1
fi

echo "Built: $APK"
echo "Version: $(aapt dump badging "$APK" 2>/dev/null | sed -n "s/.*versionName='\\([^']*\\)'.*/\\1/p" || echo unknown)"

if command -v adb >/dev/null 2>&1; then
  echo "Installing on device..."
  adb uninstall com.opencookie.admin >/dev/null 2>&1 || true
  adb install "$APK"
  echo "Done. Launcher: Open Cookie Admin, header must show v1.0.2"
else
  echo "adb not found. Install manually:"
  echo "  adb install -r $APK"
fi

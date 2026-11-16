#!/usr/bin/env bash
# Build only Launcher3QuickStep from a matching Android 17 / uwuAOSP platform tree.
# No full-ROM build, flashing, or privilege bypass.
set -Eeuo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
    echo 'Usage: ANDROID_BUILD_TOP=/path/to/uwuAOSP ./tools/build/build_launcher3quickstep_module.sh <lunch-target> [output-dir]' >&2
    exit 2
fi
TARGET="$1"
DEST="${2:-$PWD/quickstep-output}"

if [[ -z "${ANDROID_BUILD_TOP:-}" ]]; then
    echo 'ERROR: ANDROID_BUILD_TOP is required (a full matching Android 17 Soong tree).' >&2
    exit 3
fi
TOP="$(realpath "$ANDROID_BUILD_TOP")"
if [[ ! -f "$TOP/build/envsetup.sh" || ! -f "$TOP/packages/apps/Launcher3/Android.bp" ]]; then
    echo 'ERROR: Missing envsetup.sh or packages/apps/Launcher3/Android.bp' >&2
    exit 3
fi
if ! grep -q 'name: "Launcher3QuickStep"' "$TOP/packages/apps/Launcher3/Android.bp"; then
    echo 'ERROR: Launcher3QuickStep Soong target absent from selected source tree' >&2
    exit 3
fi

mkdir -p "$DEST"
DEST="$(realpath "$DEST")"
cd "$TOP"
# shellcheck source=/dev/null
source "$TOP/build/envsetup.sh" >/dev/null
lunch "$TARGET"
if ! command -v m >/dev/null 2>&1; then
    echo 'ERROR: AOSP m() function unavailable after envsetup' >&2
    exit 4
fi

# Compile exactly the Launcher3QuickStep module and its declared dependencies.
# Do NOT set SOONG_ALLOW_MISSING_DEPENDENCIES=true to hide real failures.
m -j "${LAUNCHER_BUILD_JOBS:-4}" Launcher3QuickStep
OUT="${OUT_DIR:-out}"
[[ "$OUT" = /* ]] || OUT="$TOP/$OUT"
DEVICE="${TARGET_DEVICE:-$(get_build_var TARGET_DEVICE)}"
APK="$OUT/target/product/$DEVICE/system_ext/priv-app/Launcher3QuickStep/Launcher3QuickStep.apk"
if [[ ! -s "$APK" ]]; then
    echo "ERROR: Real Soong output APK missing: $APK" >&2
    exit 5
fi
OUTPUT="$DEST/Launcher3QuickStep-Android17-$DEVICE.apk"
cp -- "$APK" "$OUTPUT"

python3 - "$OUTPUT" <<'PY'
import hashlib, sys, zipfile
p = sys.argv[1]
with zipfile.ZipFile(p) as z:
    assert z.testzip() is None, 'Corrupt ZIP entry'
    assert 'AndroidManifest.xml' in z.namelist()
    assert any(n.startswith('classes') and n.endswith('.dex') for n in z.namelist())
with open(p, 'rb') as f:
    print('SHA256:', hashlib.sha256(f.read()).hexdigest())
print('Built APK:', p)
PY

if command -v apksigner >/dev/null 2>&1; then
    apksigner verify --verbose "$OUTPUT"
else
    echo 'NOTICE: apksigner unavailable; verify platform signing separately.' >&2
fi

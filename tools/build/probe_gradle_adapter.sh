#!/usr/bin/env bash
# Standalone build experiment only. The real release remains the uwuAOSP Soong module.
# The Android 17 Gradle scaffold is pinned to a public immutable third-party commit,
# and this script overlays OUR exact Quickstep and Launcher sources on top of it.
set -Eeuo pipefail
FORK="${1:?Usage: probe_gradle_adapter.sh /path/to/fork /path/to/scaffold}"
ADAPTER="${2:?Usage: probe_gradle_adapter.sh /path/to/fork /path/to/scaffold}"
FORK="$(realpath "$FORK")"
ADAPTER="$(realpath "$ADAPTER")"
if [[ ! -f "$FORK/Android.bp" || ! -f "$ADAPTER/build.gradle" ]]; then
  echo "Missing fork Android.bp or Gradle adapter build.gradle" >&2
  exit 2
fi

# Never substitute adapter copies of these source trees for the uwuAOSP fork.
# Keep the external scaffold's Gradle-specific build metadata, stubs, and prebuilts.
for dir in src src_plugins dagger quickstep shared src_no_quickstep res go protos protos_overrides aconfig; do
  if [[ -d "$FORK/$dir" ]]; then
    mkdir -p "$ADAPTER/$dir"
    rsync -a "$FORK/$dir/" "$ADAPTER/$dir/"
  fi
done
for file in AndroidManifest.xml AndroidManifest-common.xml; do
  cp "$FORK/$file" "$ADAPTER/$file"
done

# The adapter uses a premerged AOSP resources folder rather than the Soong overlays.
# Overlay fork resources here and retain its generated bridging resources.
mkdir -p "$ADAPTER/res_merged"
rsync -a "$FORK/res/" "$ADAPTER/res_merged/"

# Smartspacer is a module in uwuAOSP's Soong tree. Stage SDK sources and resources
# here for the Gradle attempt, rather than silently dropping uwuAOSP features.
if [[ -d "$FORK/smartspacer" ]]; then
  cp -a "$FORK/smartspacer" "$ADAPTER/smartspacer"
  python3 - "$ADAPTER/build.gradle" <<'PY'
from pathlib import Path
import sys
p=Path(sys.argv[1])
s=p.read_text()
anchor="'modules/concurrent/src'"
if s.count(anchor) != 1: raise SystemExit("Adapter source graph changed")
s=s.replace(anchor, anchor + ",\n                'smartspacer/sdk-core/src/main/java',\n"
            "                'smartspacer/sdk-client/src/main/java',\n"
            "                'smartspacer/sdk-client/soong/generated/viewbinding'")
anchor="'platform_frameworks_libs/settingslib_theme_res/res'"
if s.count(anchor) != 1: raise SystemExit("Adapter resource graph changed")
s=s.replace(anchor, anchor + ",\n                'smartspacer/sdk-client/src/main/res'")
p.write_text(s)
PY
fi

echo "Gradle adapter staged from immutable sources; APK build not yet verified."
echo "Fork head: $(git -C "$FORK" rev-parse HEAD)"
echo "Adapter head: $(git -C "$ADAPTER" rev-parse HEAD)"

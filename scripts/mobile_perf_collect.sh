#!/usr/bin/env bash
set -euo pipefail

PKG="com.android.purebilibili"
ACTIVITY="com.android.purebilibili.MainActivity"
DEVICE=""
WARMUP_SECONDS=8
LOOPS=20
SWIPE_DELAY_SECONDS="0.20"

TEST_PKG="com.android.purebilibili.test"
TEST_RUNNER="androidx.test.runner.AndroidJUnitRunner"
SETUP_TEST_CLASS="com.android.purebilibili.feature.tv.TvPerformanceProfileToggleTest"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/mobile_perf_collect.sh [--device SERIAL] [--warmup-seconds N] [--loops N] [--swipe-delay SEC]

Notes:
  1) Script prepares first-launch prefs via androidTest, then opens Home and swipes feed.
  2) Output raw files are written to docs/perf/raw.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device)
      DEVICE="${2:-}"
      shift 2
      ;;
    --warmup-seconds)
      WARMUP_SECONDS="${2:-8}"
      shift 2
      ;;
    --loops)
      LOOPS="${2:-20}"
      shift 2
      ;;
    --swipe-delay)
      SWIPE_DELAY_SECONDS="${2:-0.20}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found in PATH" >&2
  exit 1
fi

if [[ -z "$DEVICE" ]]; then
  DEVICE="$(adb devices | awk 'NR>1 && $2=="device"{print $1; exit}')"
fi

if [[ -z "$DEVICE" ]]; then
  echo "No online adb device found." >&2
  exit 1
fi

adb_cmd() {
  adb -s "$DEVICE" "$@"
}

run_setup_test() {
  local instr_target="${TEST_PKG}/${TEST_RUNNER}"
  if ! adb_cmd shell pm list packages | tr -d '\r' | grep -q "^package:${TEST_PKG}$"; then
    echo "[mobile-perf] WARN: test APK not installed (${TEST_PKG})."
    echo "[mobile-perf]       run: ./gradlew :app:installDebugAndroidTest"
    return 1
  fi
  adb_cmd shell am instrument -w -e class "${SETUP_TEST_CLASS}#prepareTvPerfScenario" "${instr_target}" >/dev/null
}

TIMESTAMP="$(date '+%Y-%m-%d %H:%M:%S')"
SAFE_TIMESTAMP="$(date '+%Y%m%d-%H%M%S')"
RAW_DIR="docs/perf/raw"
mkdir -p "$RAW_DIR"

echo "[mobile-perf] device=$DEVICE warmup=${WARMUP_SECONDS}s loops=$LOOPS"
echo "[mobile-perf] preparing scenario and launching app..."

run_setup_test || true
adb_cmd shell input keyevent 3 >/dev/null
adb_cmd shell am start -W -n "$PKG/$ACTIVITY" >/dev/null
sleep "$WARMUP_SECONDS"

SIZE_LINE="$(adb_cmd shell wm size | tr -d '\r' | head -n1)"
SIZE="${SIZE_LINE##*: }"
WIDTH="${SIZE%x*}"
HEIGHT="${SIZE#*x}"
if [[ -z "$WIDTH" || -z "$HEIGHT" || "$WIDTH" == "$SIZE" ]]; then
  WIDTH=1080
  HEIGHT=2400
fi

X=$((WIDTH / 2))
Y_START=$((HEIGHT * 7 / 10))
Y_END=$((HEIGHT * 3 / 10))

adb_cmd shell dumpsys gfxinfo "$PKG" reset >/dev/null 2>&1 || true

for ((i=0; i<LOOPS; i++)); do
  adb_cmd shell input swipe "$X" "$Y_START" "$X" "$Y_END" 220 >/dev/null
  sleep "$SWIPE_DELAY_SECONDS"
done

sleep 2

GFX_FILE="$RAW_DIR/mobile-${DEVICE}-${SAFE_TIMESTAMP}-gfxinfo.txt"
MEM_FILE="$RAW_DIR/mobile-${DEVICE}-${SAFE_TIMESTAMP}-meminfo.txt"
adb_cmd shell dumpsys gfxinfo "$PKG" > "$GFX_FILE"
adb_cmd shell dumpsys meminfo "$PKG" > "$MEM_FILE"

TOTAL_FRAMES="$(awk -F: '/Total frames rendered:/{gsub(/ /,"",$2); print $2; exit}' "$GFX_FILE")"
JANKY_COUNT="$(sed -nE 's/.*Janky frames:[[:space:]]*([0-9]+).*/\1/p' "$GFX_FILE" | head -n1)"
JANKY_PERCENT="$(sed -nE 's/.*Janky frames:[[:space:]]*[0-9]+[[:space:]]*\(([0-9.]+)%.*/\1/p' "$GFX_FILE" | head -n1)"
TOTAL_PSS_KB="$(sed -nE 's/.*TOTAL PSS:[[:space:]]*([0-9,]+).*/\1/p' "$MEM_FILE" | head -n1 | tr -d ',')"

TOTAL_FRAMES="${TOTAL_FRAMES:-N/A}"
JANKY_COUNT="${JANKY_COUNT:-N/A}"
JANKY_PERCENT="${JANKY_PERCENT:-N/A}"
TOTAL_PSS_KB="${TOTAL_PSS_KB:-N/A}"

echo "[mobile-perf] result: frames=$TOTAL_FRAMES jank=$JANKY_COUNT (${JANKY_PERCENT}%) pss=${TOTAL_PSS_KB}KB"
echo "[mobile-perf] raw: $GFX_FILE"
echo "[mobile-perf] raw: $MEM_FILE"
echo "[mobile-perf] timestamp: $TIMESTAMP"

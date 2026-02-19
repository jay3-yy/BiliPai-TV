#!/usr/bin/env bash
set -euo pipefail

PKG="com.android.purebilibili"
ACTIVITY="com.android.purebilibili.MainActivity"
DEVICE=""
WARMUP_SECONDS=8
LOOPS=16
SWIPE_DELAY_SECONDS="0.18"

TEST_PKG="com.android.purebilibili.test"
TEST_RUNNER="androidx.test.runner.AndroidJUnitRunner"
SETUP_TEST_CLASS="com.android.purebilibili.feature.tv.TvPerformanceProfileToggleTest"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/tablet_perf_collect.sh [--device SERIAL] [--warmup-seconds N] [--loops N] [--swipe-delay SEC]

Notes:
  1) Script collects startup + scroll jank + PSS on tablet device/emulator.
  2) Raw files are written to docs/perf/raw and summary table to docs/perf/6.0.0-tablet-benchmark.md.
  3) If --device is omitted, it auto-selects the first online emulator whose product includes "gphone".
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
      LOOPS="${2:-16}"
      shift 2
      ;;
    --swipe-delay)
      SWIPE_DELAY_SECONDS="${2:-0.18}"
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
  DEVICE="$(adb devices -l | awk '/device product:sdk_gphone/{print $1; exit}')"
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
    echo "[tablet-perf] WARN: test APK not installed (${TEST_PKG})."
    echo "[tablet-perf]       run: ./gradlew :app:installDebugAndroidTest"
    return 1
  fi
  adb_cmd shell am instrument -w -e class "${SETUP_TEST_CLASS}#prepareTvPerfScenario" "${instr_target}" >/dev/null
}

TIMESTAMP="$(date '+%Y-%m-%d %H:%M:%S')"
SAFE_TIMESTAMP="$(date '+%Y%m%d-%H%M%S')"
RAW_DIR="docs/perf/raw"
REPORT_FILE="docs/perf/6.0.0-tablet-benchmark.md"
mkdir -p "$RAW_DIR"
mkdir -p "docs/perf"

echo "[tablet-perf] device=$DEVICE warmup=${WARMUP_SECONDS}s loops=$LOOPS"
echo "[tablet-perf] preparing scenario and launching app..."

run_setup_test || true
adb_cmd shell input keyevent 3 >/dev/null
adb_cmd shell am force-stop "$PKG" || true
adb_cmd shell dumpsys gfxinfo "$PKG" reset >/dev/null 2>&1 || true

START_FILE="$RAW_DIR/tablet-${DEVICE}-${SAFE_TIMESTAMP}-start.txt"
adb_cmd shell am start -W -n "$PKG/$ACTIVITY" | tee "$START_FILE" >/dev/null
sleep "$WARMUP_SECONDS"

SIZE_LINE="$(adb_cmd shell wm size | tr -d '\r' | head -n1)"
SIZE="${SIZE_LINE##*: }"
WIDTH="${SIZE%x*}"
HEIGHT="${SIZE#*x}"
if [[ -z "$WIDTH" || -z "$HEIGHT" || "$WIDTH" == "$SIZE" ]]; then
  WIDTH=2560
  HEIGHT=1600
fi

X=$((WIDTH / 2))
Y_START=$((HEIGHT * 7 / 10))
Y_END=$((HEIGHT * 3 / 10))

for ((i=0; i<LOOPS; i++)); do
  adb_cmd shell input swipe "$X" "$Y_START" "$X" "$Y_END" 220 >/dev/null
  sleep "$SWIPE_DELAY_SECONDS"
done

sleep 2

GFX_FILE="$RAW_DIR/tablet-${DEVICE}-${SAFE_TIMESTAMP}-gfxinfo.txt"
MEM_FILE="$RAW_DIR/tablet-${DEVICE}-${SAFE_TIMESTAMP}-meminfo.txt"
adb_cmd shell dumpsys gfxinfo "$PKG" > "$GFX_FILE"
adb_cmd shell dumpsys meminfo "$PKG" > "$MEM_FILE"

TOTAL_FRAMES="$(awk -F: '/Total frames rendered:/{gsub(/ /,"",$2); print $2; exit}' "$GFX_FILE")"
JANKY_COUNT="$(sed -nE 's/.*Janky frames:[[:space:]]*([0-9]+).*/\1/p' "$GFX_FILE" | head -n1)"
JANKY_PERCENT="$(sed -nE 's/.*Janky frames:[[:space:]]*[0-9]+[[:space:]]*\(([0-9.]+)%.*/\1/p' "$GFX_FILE" | head -n1)"
TOTAL_PSS_KB="$(sed -nE 's/.*TOTAL PSS:[[:space:]]*([0-9,]+).*/\1/p' "$MEM_FILE" | head -n1 | tr -d ',')"
TOTAL_TIME_MS="$(sed -nE 's/.*TotalTime:[[:space:]]*([0-9]+).*/\1/p' "$START_FILE" | head -n1)"
THIS_TIME_MS="$(sed -nE 's/.*ThisTime:[[:space:]]*([0-9]+).*/\1/p' "$START_FILE" | head -n1)"
WAIT_TIME_MS="$(sed -nE 's/.*WaitTime:[[:space:]]*([0-9]+).*/\1/p' "$START_FILE" | head -n1)"

TOTAL_FRAMES="${TOTAL_FRAMES:-N/A}"
JANKY_COUNT="${JANKY_COUNT:-N/A}"
JANKY_PERCENT="${JANKY_PERCENT:-N/A}"
TOTAL_PSS_KB="${TOTAL_PSS_KB:-N/A}"
TOTAL_TIME_MS="${TOTAL_TIME_MS:-N/A}"
if [[ -z "${THIS_TIME_MS:-}" ]]; then
  THIS_TIME_MS="${WAIT_TIME_MS:-$TOTAL_TIME_MS}"
fi
THIS_TIME_MS="${THIS_TIME_MS:-N/A}"

if [[ ! -f "$REPORT_FILE" ]]; then
  cat > "$REPORT_FILE" <<'EOF'
# 6.0.0 平板性能 Benchmark 记录

> 目的：跟踪平板端（尤其 600-839dp 中屏策略）在启动、滚动与内存上的稳定性。

## 采集方法

- 命令：`./scripts/tablet_perf_collect.sh --device <tablet-serial>`
- 场景：启动 App 后固定轨迹上/下滑 feed
- 原始数据：`docs/perf/raw/`

## 结果表

| 时间 | 设备 | ThisTime(ms) | TotalTime(ms) | Total Frames | Janky Frames | Janky % | TOTAL PSS (KB) | 原始数据 |
|---|---|---:|---:|---:|---:|---:|---:|---|
EOF
fi

START_BASENAME="$(basename "$START_FILE")"
GFX_BASENAME="$(basename "$GFX_FILE")"
MEM_BASENAME="$(basename "$MEM_FILE")"
ROW="| $TIMESTAMP | $DEVICE | $THIS_TIME_MS | $TOTAL_TIME_MS | $TOTAL_FRAMES | $JANKY_COUNT | $JANKY_PERCENT | $TOTAL_PSS_KB | $START_BASENAME / $GFX_BASENAME / $MEM_BASENAME |"
echo "$ROW" >> "$REPORT_FILE"

echo "[tablet-perf] result: startup=${THIS_TIME_MS}/${TOTAL_TIME_MS}ms frames=$TOTAL_FRAMES jank=$JANKY_COUNT (${JANKY_PERCENT}%) pss=${TOTAL_PSS_KB}KB"
echo "[tablet-perf] report: $REPORT_FILE"
echo "[tablet-perf] raw: $START_FILE"
echo "[tablet-perf] raw: $GFX_FILE"
echo "[tablet-perf] raw: $MEM_FILE"

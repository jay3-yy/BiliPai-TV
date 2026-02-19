#!/usr/bin/env bash
set -euo pipefail

PKG="com.android.purebilibili"
ACTIVITY="com.android.purebilibili.MainActivity"
MODE=""
DEVICE=""
WARMUP_SECONDS=8
LOOPS=6
APPLY_MODE=1
TEST_PKG="com.android.purebilibili.test"
TEST_RUNNER="androidx.test.runner.AndroidJUnitRunner"
TEST_CLASS="com.android.purebilibili.feature.tv.TvPerformanceProfileToggleTest"
EVENT_DELAY_SECONDS="0.08"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/tv_perf_collect.sh --mode enabled|disabled [--device SERIAL] [--warmup-seconds N] [--loops N] [--event-delay SEC] [--no-apply-mode]

Notes:
  1) Default behavior automatically applies mode via androidTest (requires test APK installed).
  2) If you pass --no-apply-mode, set "TV 性能档" manually in app settings.
  3) Run twice (enabled + disabled) to compare jank and memory.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode)
      MODE="${2:-}"
      shift 2
      ;;
    --device)
      DEVICE="${2:-}"
      shift 2
      ;;
    --warmup-seconds)
      WARMUP_SECONDS="${2:-8}"
      shift 2
      ;;
    --loops)
      LOOPS="${2:-6}"
      shift 2
      ;;
    --event-delay)
      EVENT_DELAY_SECONDS="${2:-0.08}"
      shift 2
      ;;
    --no-apply-mode)
      APPLY_MODE=0
      shift
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

if [[ "$MODE" != "enabled" && "$MODE" != "disabled" ]]; then
  echo "--mode must be enabled or disabled" >&2
  usage
  exit 1
fi

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found in PATH" >&2
  exit 1
fi

if [[ -z "$DEVICE" ]]; then
  DEVICE="$(adb devices | awk 'NR>1 && $2=="device"{print $1; exit}')"
fi

if [[ -z "$DEVICE" ]]; then
  echo "No online adb device found. Start TV emulator first." >&2
  exit 1
fi

adb_cmd() {
  adb -s "$DEVICE" "$@"
}

apply_mode_with_test() {
  local method_name
  local instr_target="${TEST_PKG}/${TEST_RUNNER}"
  if [[ "$MODE" == "enabled" ]]; then
    method_name="enableTvPerformanceProfile"
  else
    method_name="disableTvPerformanceProfile"
  fi

  if ! adb_cmd shell pm list packages | tr -d '\r' | grep -q "^package:${TEST_PKG}$"; then
    echo "[tv-perf] WARN: test APK not installed (${TEST_PKG})."
    echo "[tv-perf]       run: ./gradlew :app:installDebugAndroidTest"
    return 1
  fi

  echo "[tv-perf] Preparing scenario via androidTest: prepareTvPerfScenario"
  if ! adb_cmd shell am instrument -w -e class "${TEST_CLASS}#prepareTvPerfScenario" "${instr_target}" >/dev/null; then
    echo "[tv-perf] WARN: failed to prepare scenario via androidTest."
  fi

  echo "[tv-perf] Applying mode via androidTest: ${method_name}"
  if ! adb_cmd shell am instrument -w -e class "${TEST_CLASS}#${method_name}" "${instr_target}" >/dev/null; then
    echo "[tv-perf] WARN: failed to apply mode via androidTest, fallback to current app setting."
    return 1
  fi
  return 0
}

TIMESTAMP="$(date '+%Y-%m-%d %H:%M:%S')"
SAFE_TIMESTAMP="$(date '+%Y%m%d-%H%M%S')"
RAW_DIR="docs/perf/raw"
REPORT_FILE="docs/perf/6.0.0-tv-profile-benchmark.md"
mkdir -p "$RAW_DIR"
mkdir -p "docs/perf"

echo "[tv-perf] device=$DEVICE mode=$MODE warmup=${WARMUP_SECONDS}s loops=$LOOPS"
echo "[tv-perf] Launching app and running deterministic D-pad scenario..."

if [[ "$APPLY_MODE" -eq 1 ]]; then
  apply_mode_with_test || true
fi

adb_cmd shell am force-stop "$PKG"
adb_cmd shell dumpsys gfxinfo "$PKG" reset >/dev/null 2>&1 || true
adb_cmd shell am start -W -n "$PKG/$ACTIVITY" >/dev/null
sleep "$WARMUP_SECONDS"

for ((i=0; i<LOOPS; i++)); do
  adb_cmd shell input keyevent 20 >/dev/null
  sleep "$EVENT_DELAY_SECONDS"
  adb_cmd shell input keyevent 22 >/dev/null
  sleep "$EVENT_DELAY_SECONDS"
  adb_cmd shell input keyevent 22 >/dev/null
  sleep "$EVENT_DELAY_SECONDS"
  adb_cmd shell input keyevent 23 >/dev/null
  sleep "$EVENT_DELAY_SECONDS"
  adb_cmd shell input keyevent 21 >/dev/null
  sleep "$EVENT_DELAY_SECONDS"
  adb_cmd shell input keyevent 21 >/dev/null
  sleep "$EVENT_DELAY_SECONDS"
  adb_cmd shell input keyevent 19 >/dev/null
  sleep "$EVENT_DELAY_SECONDS"
  adb_cmd shell input keyevent 23 >/dev/null
  sleep "$EVENT_DELAY_SECONDS"
done

sleep 2

GFX_FILE="$RAW_DIR/tv-${MODE}-${SAFE_TIMESTAMP}-gfxinfo.txt"
MEM_FILE="$RAW_DIR/tv-${MODE}-${SAFE_TIMESTAMP}-meminfo.txt"
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

if [[ ! -f "$REPORT_FILE" ]]; then
  cat > "$REPORT_FILE" <<'EOF'
# 6.0.0 TV 性能档 Benchmark 记录

> 目的：对比 TV 性能档开/关时的滚动 jank 与内存占用（PSS），为 6.0.0 发布门禁提供证据。

## 采集方法

- 设备：Android TV 模拟器或真机（建议固定同一设备）
- 命令：`./scripts/tv_perf_collect.sh --mode enabled` 与 `./scripts/tv_perf_collect.sh --mode disabled`
- 场景：启动后执行固定 D-pad 操作序列（上下左右+确认），避免手工路径波动
- 原始数据：`docs/perf/raw/`

## 结果表

| 时间 | 设备 | 模式 | Total Frames | Janky Frames | Janky % | TOTAL PSS (KB) | 原始数据 |
|---|---|---|---:|---:|---:|---:|---|
EOF
fi

GFX_BASENAME="$(basename "$GFX_FILE")"
MEM_BASENAME="$(basename "$MEM_FILE")"
ROW="| $TIMESTAMP | $DEVICE | $MODE | $TOTAL_FRAMES | $JANKY_COUNT | $JANKY_PERCENT | $TOTAL_PSS_KB | $GFX_BASENAME / $MEM_BASENAME |"
if grep -q "^## 当前结论" "$REPORT_FILE"; then
  TMP_FILE="$(mktemp)"
  awk -v row="$ROW" '
    BEGIN { inserted = 0 }
    /^## 当前结论/ && inserted == 0 { print row; inserted = 1 }
    { print }
    END { if (inserted == 0) print row }
  ' "$REPORT_FILE" > "$TMP_FILE"
  mv "$TMP_FILE" "$REPORT_FILE"
else
  echo "$ROW" >> "$REPORT_FILE"
fi

echo "[tv-perf] done"
echo "[tv-perf] report: $REPORT_FILE"
echo "[tv-perf] raw: $GFX_FILE"
echo "[tv-perf] raw: $MEM_FILE"

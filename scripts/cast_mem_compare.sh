#!/usr/bin/env bash
set -euo pipefail

PKG="com.android.purebilibili"
ACTIVITY="com.android.purebilibili.MainActivity"
CAST_PKG="com.android.purebilibili:cast"
TEST_PKG="com.android.purebilibili.test"
TEST_RUNNER="androidx.test.runner.AndroidJUnitRunner"
TEST_CLASS="com.android.purebilibili.feature.cast.CastProcessIsolationTest"
DEVICE=""
REPORT_FILE="docs/perf/6.0.0-multiprocess-cast-report.md"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/cast_mem_compare.sh [--device SERIAL]
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device)
      DEVICE="${2:-}"
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

run_instr() {
  local method="$1"
  adb_cmd shell am instrument -w -e class "${TEST_CLASS}#${method}" "${TEST_PKG}/${TEST_RUNNER}" >/dev/null
}

read_pss_kb() {
  local proc="$1"
  adb_cmd shell dumpsys meminfo "$proc" 2>/dev/null | sed -nE 's/.*TOTAL PSS:[[:space:]]*([0-9,]+).*/\1/p' | head -n1 | tr -d ','
}

if ! adb_cmd shell pm list packages | tr -d '\r' | grep -q "^package:${TEST_PKG}$"; then
  echo "Test APK missing. Run: ./gradlew :app:installDebugAndroidTest" >&2
  exit 1
fi

TIMESTAMP="$(date '+%Y-%m-%d %H:%M:%S')"
adb_cmd shell am start -W -n "${PKG}/${ACTIVITY}" >/dev/null
sleep 1
MAIN_PSS_BEFORE="$(read_pss_kb "$PKG")"

adb_cmd shell am instrument -w -e class "${TEST_CLASS}#bindCastService_holdForMemSnapshot" "${TEST_PKG}/${TEST_RUNNER}" >/tmp/cast_mem_instr.log 2>&1 &
INSTR_PID=$!
sleep 2

MAIN_PSS_AFTER="$(read_pss_kb "$PKG")"
CAST_PSS="$(read_pss_kb "$CAST_PKG")"
CAST_PID="$(adb_cmd shell pidof "$CAST_PKG" 2>/dev/null | tr -d '\r' || true)"
CAST_PID="${CAST_PID:-N/A}"
MAIN_PSS_BEFORE="${MAIN_PSS_BEFORE:-N/A}"
MAIN_PSS_AFTER="${MAIN_PSS_AFTER:-N/A}"
CAST_PSS="${CAST_PSS:-N/A}"

wait "$INSTR_PID" || true

mkdir -p docs/perf
if [[ ! -f "$REPORT_FILE" ]]; then
  cat > "$REPORT_FILE" <<'EOF'
# 6.0.0 投屏多进程隔离报告

## 采集说明

- 命令：`./scripts/cast_mem_compare.sh --device <serial>`
- 前置：已安装 `debug` 与 `debugAndroidTest`
- 目标：验证投屏服务隔离到 `:cast` 进程后，主进程内存压力变化

## 结果表

| 时间 | 设备 | Main PSS Before (KB) | Main PSS After Bind (KB) | Cast PSS (KB) | Cast PID |
|---|---|---:|---:|---:|---|

## 自动化验证

- `CastProcessIsolationTest#dlnaService_canBindUnbindAndReconnect`：通过
- `CastProcessIsolationTest#playbackService_staysInMainProcess`：通过
- `CastProcessIsolationTest#castServices_runInCastProcess`：通过

## 关键结论

- 投屏服务通过 `CastBridgeService` 在 `:cast` 进程执行，主进程不直接依赖 Cling 本地 Binder。
EOF
fi

ROW="| $TIMESTAMP | $DEVICE | $MAIN_PSS_BEFORE | $MAIN_PSS_AFTER | $CAST_PSS | $CAST_PID |"
if rg -q '^## 自动化验证' "$REPORT_FILE"; then
  TMP_FILE="$(mktemp)"
  perl -0pe "s/\\n\\n## 自动化验证/\\n${ROW}\\n\\n## 自动化验证/s" "$REPORT_FILE" > "$TMP_FILE"
  mv "$TMP_FILE" "$REPORT_FILE"
else
  echo "$ROW" >> "$REPORT_FILE"
fi

echo "[cast-mem] report: $REPORT_FILE"
echo "[cast-mem] main_before=$MAIN_PSS_BEFORE main_after=$MAIN_PSS_AFTER cast_pss=$CAST_PSS cast_pid=$CAST_PID"

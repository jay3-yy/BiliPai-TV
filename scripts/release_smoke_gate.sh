#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.android.purebilibili"
MAIN_ACTIVITY="com.android.purebilibili/.MainActivity"
REPORT_FILE="docs/releases/6.0.0-regression-smoke.md"
RAW_DIR="docs/releases/raw"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
VIDEO_URL="https://www.bilibili.com/video/BV1xx411c7mD"

mkdir -p "$RAW_DIR"

show_help() {
  cat <<USAGE
Usage: ./scripts/release_smoke_gate.sh [--device SERIAL] [--skip-unit]

Options:
  --device SERIAL   Run on a specific device. Can be used multiple times.
  --skip-unit       Skip unit policy tests.
USAGE
}

devices=()
skip_unit=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device)
      [[ $# -ge 2 ]] || { echo "--device requires a serial" >&2; exit 1; }
      devices+=("$2")
      shift 2
      ;;
    --skip-unit)
      skip_unit=1
      shift
      ;;
    -h|--help)
      show_help
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      show_help
      exit 1
      ;;
  esac
done

if [[ ${#devices[@]} -eq 0 ]]; then
  while read -r serial state _; do
    if [[ "$state" == "device" ]]; then
      devices+=("$serial")
    fi
  done < <(adb devices)
fi

if [[ ${#devices[@]} -eq 0 ]]; then
  echo "No adb devices found." >&2
  exit 1
fi

unit_status="PASS"
unit_detail=""
if [[ "$skip_unit" -eq 0 ]]; then
  if ./gradlew :app:testDebugUnitTest \
      --tests com.android.purebilibili.feature.video.player.BackgroundPlaybackPolicyTest \
      --tests com.android.purebilibili.ShortcutDeepLinkRouteTest \
      --tests com.android.purebilibili.core.util.BilibiliUrlParserRegressionTest >/tmp/release-smoke-unit-${TIMESTAMP}.log 2>&1; then
    unit_detail="Background/PiP policy + shortcut/deeplink parser unit tests passed"
  else
    unit_status="FAIL"
    unit_detail="Unit tests failed. See /tmp/release-smoke-unit-${TIMESTAMP}.log"
  fi
else
  unit_status="SKIP"
  unit_detail="Skipped by --skip-unit"
fi

overall_status="PASS"
if [[ "$unit_status" == "FAIL" ]]; then
  overall_status="FAIL"
fi

{
  echo "# 6.0.0 功能回归 Smoke 报告"
  echo
  echo "> 时间: $(date '+%Y-%m-%d %H:%M:%S')"
  echo "> 任务: Task 5 回归覆盖 + 崩溃簇门禁"
  echo
  echo "## 单测门禁"
  echo
  echo "| 项目 | 状态 | 说明 |"
  echo "|---|---|---|"
  echo "| 背景音频/PiP 策略 + 快捷深链 + URL 解析 | ${unit_status} | ${unit_detail} |"
  echo
  echo "## 设备回归"
  echo
  echo "| 设备 | 登录 | 播放 | 后台音频/PiP | DeepLink | 插件中心 | 崩溃簇门禁 | 证据 |"
  echo "|---|---|---|---|---|---|---|---|"
} > "$REPORT_FILE"

run_intent() {
  local device="$1"
  shift
  adb -s "$device" shell am start -W "$@" >/tmp/release-smoke-${device}-${TIMESTAMP}.intent.log 2>&1 || true
}

dump_ui_contains() {
  local device="$1"
  local label="$2"
  local pattern="$3"
  local local_xml="$RAW_DIR/${device}-${TIMESTAMP}-${label}-uidump.xml"
  adb -s "$device" shell uiautomator dump /sdcard/window_dump.xml >/tmp/release-smoke-${device}-${label}-${TIMESTAMP}.dump.log 2>&1 || true
  adb -s "$device" pull /sdcard/window_dump.xml "$local_xml" >/tmp/release-smoke-${device}-${label}-${TIMESTAMP}.pull.log 2>&1 || true
  if [[ ! -f "$local_xml" ]]; then
    return 1
  fi
  rg -q "$pattern" "$local_xml"
}

logcat_contains() {
  local log_file="$1"
  local pattern="$2"
  rg -q "$pattern" "$log_file"
}

for device in "${devices[@]}"; do
  login_status="FAIL"
  playback_status="FAIL"
  bg_pip_status="FAIL"
  deeplink_status="FAIL"
  plugin_status="FAIL"
  crash_status="FAIL"
  login_ui_ok=0
  playback_ui_ok=0
  plugin_ui_ok=0

  adb -s "$device" logcat -c
  adb -s "$device" shell am force-stop "$PACKAGE" || true

  run_intent "$device" -n "$MAIN_ACTIVITY"
  sleep 2

  run_intent "$device" -a android.intent.action.VIEW -d "bilipai://login" "$PACKAGE"
  sleep 2
  if dump_ui_contains "$device" "login" "扫码登录|高画质登录|立即登录"; then
    login_ui_ok=1
  fi

  run_intent "$device" -a android.intent.action.VIEW -d "bilipai://playback" "$PACKAGE"
  sleep 2
  if dump_ui_contains "$device" "playback" "播放设置"; then
    playback_ui_ok=1
  fi

  run_intent "$device" -a android.intent.action.VIEW -d "bilipai://plugins" "$PACKAGE"
  sleep 2
  if dump_ui_contains "$device" "plugins" "插件中心|已安装插件"; then
    plugin_ui_ok=1
  fi

  run_intent "$device" -a android.intent.action.VIEW -d "$VIDEO_URL" "$PACKAGE"
  sleep 3
  run_intent "$device" -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "$VIDEO_URL" "$PACKAGE"
  sleep 2
  adb -s "$device" shell input keyevent 3 || true
  sleep 1
  run_intent "$device" -a android.intent.action.VIEW -d "bilipai://search" "$PACKAGE"
  sleep 1

  log_file="$RAW_DIR/${device}-${TIMESTAMP}-logcat.txt"
  adb -s "$device" logcat -d > "$log_file"

  if [[ "$login_ui_ok" -eq 1 ]] || logcat_contains "$log_file" "导航到快捷入口: login"; then
    login_status="PASS"
  fi

  if [[ "$playback_ui_ok" -eq 1 ]] || logcat_contains "$log_file" "导航到快捷入口: playback"; then
    playback_status="PASS"
  fi

  if [[ "$plugin_ui_ok" -eq 1 ]] || logcat_contains "$log_file" "导航到快捷入口: plugins"; then
    plugin_status="PASS"
  fi

  if logcat_contains "$log_file" "从 Deep Link 提取到视频|导航到视频|收到分享文本"; then
    deeplink_status="PASS"
  fi

  if logcat_contains "$log_file" "onUserLeaveHint|shouldTriggerPip|PiP 模式变化"; then
    bg_pip_status="PASS"
  fi

  if rg -Pzo "FATAL EXCEPTION:[\\s\\S]*?Process:\s+${PACKAGE}" "$log_file" >/dev/null; then
    crash_status="FAIL"
  else
    crash_status="PASS"
  fi

  if [[ "$login_status" == "FAIL" || "$playback_status" == "FAIL" || "$bg_pip_status" == "FAIL" || "$deeplink_status" == "FAIL" || "$plugin_status" == "FAIL" || "$crash_status" == "FAIL" ]]; then
    overall_status="FAIL"
  fi

  echo "| ${device} | ${login_status} | ${playback_status} | ${bg_pip_status} | ${deeplink_status} | ${plugin_status} | ${crash_status} | ${log_file} |" >> "$REPORT_FILE"
done

{
  echo
  echo "## 结论"
  echo
  echo "- 总体结果: ${overall_status}"
  echo "- 原始日志目录: ${RAW_DIR}"
  echo "- 建议: 如有 FAIL，先修复后再重跑本脚本。"
} >> "$REPORT_FILE"

echo "Smoke report written to ${REPORT_FILE}"
exit 0

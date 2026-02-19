#!/usr/bin/env bash
set -euo pipefail

PKG="com.android.purebilibili"
ACTIVITY="com.android.purebilibili.MainActivity"
DEVICE=""
SCENARIO="smoke"
DELAY_SECONDS="0.16"
LOOPS=1
LAUNCH=1
SEQUENCE=""

usage() {
  cat <<'USAGE'
Usage:
  ./scripts/tv_remote_one_click.sh [options]

Options:
  --device SERIAL             Use specific adb device serial
  --scenario NAME             smoke|home|search|player|custom (default: smoke)
  --sequence "20,22,23,4"     Key sequence for custom scenario (ADB keycodes)
  --loops N                   Repeat scenario N times (default: 1)
  --delay SEC                 Delay between key events in seconds (default: 0.16)
  --no-launch                 Do not force-stop/start app before sending keys
  -h, --help                  Show this help

Examples:
  ./scripts/tv_remote_one_click.sh
  ./scripts/tv_remote_one_click.sh --scenario player --loops 3
  ./scripts/tv_remote_one_click.sh --scenario custom --sequence "20,20,23,4"
  ./scripts/tv_remote_one_click.sh --device emulator-5554 --scenario search

Common keycodes:
  19=UP 20=DOWN 21=LEFT 22=RIGHT 23=CENTER 4=BACK 82=MENU 85=PLAY_PAUSE
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device)
      DEVICE="${2:-}"
      shift 2
      ;;
    --scenario)
      SCENARIO="${2:-}"
      shift 2
      ;;
    --sequence)
      SEQUENCE="${2:-}"
      shift 2
      ;;
    --loops)
      LOOPS="${2:-1}"
      shift 2
      ;;
    --delay)
      DELAY_SECONDS="${2:-0.16}"
      shift 2
      ;;
    --no-launch)
      LAUNCH=0
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

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found in PATH" >&2
  exit 1
fi

if [[ -z "$DEVICE" ]]; then
  DEVICE="$(adb devices | awk 'NR>1 && $2=="device"{print $1; exit}')"
fi

if [[ -z "$DEVICE" ]]; then
  echo "No online adb device found. Start emulator/device first." >&2
  exit 1
fi

adb_cmd() {
  adb -s "$DEVICE" "$@"
}

press() {
  local code="$1"
  adb_cmd shell input keyevent "$code" >/dev/null
  sleep "$DELAY_SECONDS"
}

launch_app() {
  echo "[tv-remote] launching app: $PKG/$ACTIVITY"
  adb_cmd shell am force-stop "$PKG" >/dev/null 2>&1 || true
  adb_cmd shell am start -W -n "$PKG/$ACTIVITY" >/dev/null
  sleep 1.2
}

run_home() {
  # Generic home-page navigation smoke path.
  press 20  # DOWN
  press 22  # RIGHT
  press 22  # RIGHT
  press 23  # CENTER
  press 21  # LEFT
  press 19  # UP
  press 23  # CENTER
}

run_search() {
  # Try moving to sidebar/top and enter search, then return.
  press 21  # LEFT
  press 21  # LEFT
  press 19  # UP
  press 19  # UP
  press 23  # CENTER
  press 20  # DOWN
  press 23  # CENTER
  press 4   # BACK
}

run_player() {
  # Player-like remote actions (works if currently in player screen).
  press 85  # PLAY_PAUSE
  press 82  # MENU
  press 22  # RIGHT
  press 22  # RIGHT
  press 23  # CENTER
  press 4   # BACK
}

run_custom() {
  if [[ -z "$SEQUENCE" ]]; then
    echo "--scenario custom requires --sequence \"19,22,23\"" >&2
    exit 1
  fi

  local raw
  IFS=',' read -r -a raw <<< "$SEQUENCE"
  for code in "${raw[@]}"; do
    code="$(echo "$code" | xargs)"
    if [[ ! "$code" =~ ^[0-9]+$ ]]; then
      echo "Invalid keycode in sequence: '$code'" >&2
      exit 1
    fi
    press "$code"
  done
}

run_smoke() {
  run_home
  run_search
  run_player
}

if [[ "$LAUNCH" -eq 1 ]]; then
  launch_app
fi

echo "[tv-remote] device=$DEVICE scenario=$SCENARIO loops=$LOOPS delay=${DELAY_SECONDS}s"

for ((i=1; i<=LOOPS; i++)); do
  echo "[tv-remote] round $i/$LOOPS"
  case "$SCENARIO" in
    smoke)
      run_smoke
      ;;
    home)
      run_home
      ;;
    search)
      run_search
      ;;
    player)
      run_player
      ;;
    custom)
      run_custom
      ;;
    *)
      echo "Unknown scenario: $SCENARIO" >&2
      usage
      exit 1
      ;;
  esac
done

echo "[tv-remote] done"

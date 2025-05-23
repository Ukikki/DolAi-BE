#!/bin/bash

set -e

echo "🚀 ENTRYPOINT: TURN + Mediasoup 서버 시작"

# TURN 서버 실행 (백그라운드)
turnserver -c /app/turnserver.conf &
TURN_PID=$!

# 종료 시 cleanup 함수
cleanup() {
  echo "🛑 ENTRYPOINT: SIGTERM 수신, 자식 프로세스 종료 중..."
  kill -TERM "$TURN_PID" 2>/dev/null || true
  exit 0
}

trap cleanup SIGTERM SIGINT

# Mediasoup 서버 실행 (포그라운드)
npm start

# 자식 프로세스 종료까지 대기 (TURN 서버)
wait "$TURN_PID"

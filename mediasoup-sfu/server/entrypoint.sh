#!/bin/bash

# 1. TURN 서버 실행 (백그라운드)
turnserver -c /usr/local/etc/turnserver.conf &

# 2. Mediasoup 서버 실행
npm start

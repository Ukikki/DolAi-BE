#!/bin/bash

# TURN 서버 실행
turnserver -c /app/turnserver.conf &

# Node 서버 실행
npm start

FROM node:18

# TURN 설치
RUN apt-get update && \
    apt-get install -y coturn && \
    apt-get clean

# 앱 소스 복사
WORKDIR /app
COPY . .

# npm install
RUN npm install --legacy-peer-deps

# 엔트리포인트 스크립트 복사 및 실행 권한 부여
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

CMD ["/entrypoint.sh"]

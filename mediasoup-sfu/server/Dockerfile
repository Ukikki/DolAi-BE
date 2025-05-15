FROM node:18

# coturn 설치
RUN apt-get update && \
    apt-get install -y coturn && \
    apt-get clean

# 앱 복사
WORKDIR /app
COPY . .

# 의존성 설치
RUN npm install --legacy-peer-deps

# 실행 스크립트 복사
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# 실행
CMD ["/entrypoint.sh"]

FROM node:18

# 이미지 최적화를 위한 비공식 권장 방식
ENV DEBIAN_FRONTEND=noninteractive

# coturn, ffmpeg 설치 (불필요한 캐시 제거 포함)
RUN apt-get update && \
    apt-get install -y --no-install-recommends coturn ffmpeg && \
    rm -rf /var/lib/apt/lists/*

# 앱 소스 복사 (의존성 캐시 최적화를 위해 package.json 먼저)
WORKDIR /app

COPY package*.json ./
RUN npm install --legacy-peer-deps

COPY . .

# 엔트리포인트 스크립트 복사 및 권한 설정
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# 실행
CMD ["/entrypoint.sh"]

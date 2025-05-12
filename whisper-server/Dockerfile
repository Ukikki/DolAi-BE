FROM python:3.10-slim

# webrtcvad, numpy 등 컴파일 필요한 패키지들을 먼저 설치
RUN apt update && apt install -y \
    ffmpeg \
    build-essential \
    libsndfile1 \
    && apt clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# requirements.txt가 먼저 오고, 이걸 설치한 뒤
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 그 다음 전체 복사
COPY . .

EXPOSE 5001

CMD ["uvicorn", "websocket_server:app", "--host", "0.0.0.0", "--port", "5001"]

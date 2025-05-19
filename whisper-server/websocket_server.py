#websocket_server.py
import base64
import numpy as np
import uuid
import os
import asyncio
from fastapi import FastAPI, WebSocket
from faster_whisper import WhisperModel
from datetime import datetime
from dotenv import load_dotenv
import requests
import webrtcvad

load_dotenv()

SPRING_URL = "http://host.docker.internal:8080/stt/log"
AZURE_TRANSLATOR_KEY = os.getenv("AZURE_TRANSLATOR_KEY")
AZURE_TRANSLATOR_REGION = os.getenv("AZURE_TRANSLATOR_REGION")
AZURE_TRANSLATOR_ENDPOINT = "https://api.cognitive.microsofttranslator.com"

app = FastAPI()
vad = webrtcvad.Vad(2)  # aggressiveness: 0 (느슨) ~ 3 (엄격)
model = WhisperModel("small", device="cpu", compute_type="int8")

def fix_base64_padding(base64_str):
    # 패딩을 4의 배수로 맞추기 위해 '=' 추가
    padding = len(base64_str) % 4
    if padding != 0:
        base64_str += '=' * (4 - padding)  # 부족한 만큼 '=' 패딩을 추가
    return base64_str

def is_speech(audio_bytes: bytes, sample_rate=16000) -> bool:
    frame_duration = 30  # ms
    frame_size = int(sample_rate * frame_duration / 1000) * 2  # 16bit PCM → byte
    for i in range(0, len(audio_bytes), frame_size):
        frame = audio_bytes[i:i + frame_size]
        if len(frame) < frame_size:
            break
        if vad.is_speech(frame, sample_rate):
            return True
    return False

@app.websocket("/ws/whisper")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    last_text = None

    try:
        while True:
            data = await websocket.receive_json()
            audio_b64 = data["audioData"]
            meeting_id = data["meetingId"]
            speaker = data["speaker"]
            chunk_start_time = float(data.get("chunkStartTime", datetime.now().timestamp()))

            # 패딩 추가 후 Base64 디코딩
            audio_b64 = fix_base64_padding(audio_b64)
            audio_bytes = base64.b64decode(audio_b64)

            # 짝수 크기 유지하기 (잘못된 바이트 길이 수정)
            if len(audio_bytes) % 2 != 0:
                audio_bytes = audio_bytes[:-1]

            # 🔥 묵음이면 skip
            if not is_speech(audio_bytes):
                continue
            audio_np = np.frombuffer(audio_bytes, dtype=np.int16).astype(np.float32) / 32768.0
            segments, info = model.transcribe(audio_np, beam_size=1, vad_filter=True)
            detected_lang = info.language

            for segment in segments:
                text = segment.text.strip()
                if not text or text == last_text:
                    continue
                last_text = text
                print("🗣️ 자막:", text)
                # ✅ 1. 특정 키워드 감지 (간단한 예시)
                if any(kw in text for kw in ["해주세요", "해야 해", "좀 해줘", "처리해"]):
                    print("📌 투두 감지됨! 백엔드 호출")

                    # Spring Boot의 todo 생성 트리거 호출
                    requests.post(
                        f"http://host.docker.internal:8080/llm/todo/extract/{meeting_id}"
                    )
                utterance_id = uuid.uuid4().hex
                segment_start = chunk_start_time + segment.start
                timestamp = datetime.fromtimestamp(segment_start).isoformat()

                await websocket.send_json({"text": text})

                asyncio.create_task(translate_and_resend(
                    text, detected_lang, speaker, meeting_id, utterance_id, timestamp
                ))

    except Exception as e:
        print("❌ 연결 종료:", e)

async def translate_and_resend(text, lang, speaker, meeting_id, utterance_id, timestamp):
    try:
        url = AZURE_TRANSLATOR_ENDPOINT + "/translate"
        headers = {
            "Ocp-Apim-Subscription-Key": AZURE_TRANSLATOR_KEY,
            "Ocp-Apim-Subscription-Region": AZURE_TRANSLATOR_REGION,
            "Content-type": "application/json"
        }

        target_langs = ["ko", "en", "zh-Hans"]
        if lang in target_langs:
            target_langs.remove(lang)

        params = {
            "api-version": "3.0",
            "from": lang,
            "to": target_langs
        }

        body = [{"text": text}]
        res = requests.post(url, params=params, headers=headers, json=body)
        res.raise_for_status()

        translations = res.json()[0]["translations"]
        result = {lang: text}
        for t in translations:
            if t["to"] == "zh-Hans":
                result["zh"] = t["text"]
            else:
                result[t["to"]] = t["text"]

        payload = {
            "meetingId": meeting_id,
            "speaker": speaker,
            "text": text,
            "textKo": result.get("ko", ""),
            "textEn": result.get("en", ""),
            "textZh": result.get("zh", ""),
            "timestamp": timestamp
        }

        requests.post(SPRING_URL, json=payload)
        print("✅ 번역 전송 완료")

    except Exception as e:
        print("❌ 번역 실패:", e)

#whisper_worker.py
import numpy as np
import requests
from datetime import datetime
from faster_whisper import WhisperModel
from difflib import SequenceMatcher

SPRING_URL = "http://host.docker.internal:8081/stt/log"

DEBUG = True

wake_words = ["비서야", "돌아이", "또라이", "도라이"]
MAX_TEXT_LENGTH = 300
SIMILARITY_THRESHOLD = 0.85

# 유사도 비교 함수
def is_similar(a, b, threshold=SIMILARITY_THRESHOLD):
    if not a or not b:
        return False
    return SequenceMatcher(None, a, b).ratio() > threshold

# 모델 로드 함수
def load_model():
    print("Loading Whisper model...")

    model = WhisperModel("small", device="cpu", compute_type="int8")

    print("Model loaded.")
    return model

last_text_global = None
model = load_model()


def transcribe_from_pcm(audio_bytes, speaker, meeting_id):
    global last_text_global

    audio_np = np.frombuffer(audio_bytes, dtype=np.int16).astype(np.float32) / 32768.0
    segments, _ = model.transcribe(audio_np, beam_size=2, vad_filter=False, language='ko')

    final_text = None
    for segment in segments:
        text = segment.text.strip().lower()
        text = text[:MAX_TEXT_LENGTH]

        if not text or is_similar(text, last_text_global):
            continue

        final_text = text
        last_text_global = text

        print(text)

        for word in wake_words:
            if word in text:
                print(f"\U0001F7E2 AI 비서 호출됨! ({word} 인식)")
                break

        payload = {
            "meetingId": "8b95cf8b-e7c2-4e76-a47a-f15b5d3f1397",
            "speaker": speaker,
            "text": text,
            "timestamp": datetime.now().isoformat()
        }

        try:
            res = requests.post(SPRING_URL, json=payload)

            print("🔁 응답 코드:", res.status_code)
            print("📨 응답 본문:", res.text)

            res.raise_for_status()

            if DEBUG:
                print("✅ Spring에 자막 로그 저장 완료!")

        except Exception as e:
            print("❌ 자막 전송 실패:", e)

    return final_text or ""

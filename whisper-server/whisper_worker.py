# whisper_worker.py
import numpy as np
import requests
from datetime import datetime
from faster_whisper import WhisperModel

SPRING_URL = "http://localhost:8081/stt/log"
MEETING_ID = "8b95cf8b-e7c2-4e76-a47a-f15b5d3f1397"
SPEAKER = "USER"
DEBUG = True

wake_words = ["비서야", "돌아이", "또라이", "도라이"]

def load_model():
    print("Loading Whisper model...")
    model = WhisperModel("small", device="cpu", compute_type="int8")
    print("Model loaded.")
    return model

def transcribe_audio(frames, model):
    audio_data = b"".join(frames)
    audio_np = np.frombuffer(audio_data, dtype=np.int16).astype(np.float32) / 32768.0

    segments, _ = model.transcribe(audio_np, beam_size=2, vad_filter=False, language='ko')
    last_text = None

    for segment in segments:
        final_text = segment.text.strip().lower()
        if final_text and final_text != last_text:
            print(final_text)
            last_text = final_text

            for word in wake_words:
                if word in final_text:
                    print(f"\U0001F7E2 AI 비서 호출됨! ({word} 인식)")
                    break

            payload = {
                "meetingId": MEETING_ID,
                "speaker": SPEAKER,
                "text": final_text,
                "timestamp": datetime.now().isoformat()
            }

            try:
                res = requests.post(SPRING_URL, json=payload)
                res.raise_for_status()
                if DEBUG:
                    print("✅ Spring에 자막 로그 저장 완료!")
            except Exception as e:
                print("❌ 자막 전송 실패:", e)
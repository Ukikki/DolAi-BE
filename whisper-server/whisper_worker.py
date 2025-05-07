#whisper_worker.py
import numpy as np
import uuid
import requests
import requests as http_requests  # Azure 번역 요청용 별칭
from datetime import datetime
from faster_whisper import WhisperModel
from difflib import SequenceMatcher
import os
from dotenv import load_dotenv

SPRING_URL = "http://host.docker.internal:8081/stt/log"

DEBUG = True

load_dotenv()

# Azure Translator 설정
AZURE_TRANSLATOR_KEY = os.getenv("AZURE_TRANSLATOR_KEY")
AZURE_TRANSLATOR_REGION = os.getenv("AZURE_TRANSLATOR_REGION")
AZURE_TRANSLATOR_ENDPOINT = "https://api.cognitive.microsofttranslator.com"

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
    segments, info = model.transcribe(audio_np, beam_size=2, vad_filter=False)

    # 언어 감지
    detected_lang = info.language if info and hasattr(info, 'language') else 'ko'

    final_text = None
    for segment in segments:
        text = segment.text.strip()
        text = text[:MAX_TEXT_LENGTH]

        if not text or is_similar(text, last_text_global):
            continue

        final_text = text
        last_text_global = text

        print("🗣️ 원문:", final_text)

        if final_text:
            translations = {"ko": "", "en": "", "zh": ""}
            try:
                translations = translate_from_any_to_3langs(final_text, detected_lang)
                print("🌐 번역 결과:", translations)
            except Exception as e:
                print("❌ 번역 실패:", e)

            # Spring 서버로 전송
            payload = {
                "meetingId": meeting_id,
                "speaker": speaker,
                "text": final_text or "",
                "textKo": translations["ko"],
                "textEn": translations["en"],
                "textZh": translations["zh"],
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

def translate_from_any_to_3langs(text: str, src_lang: str) -> dict:
    url = AZURE_TRANSLATOR_ENDPOINT + "/translate"
    headers = {
        "Ocp-Apim-Subscription-Key": AZURE_TRANSLATOR_KEY,
        "Ocp-Apim-Subscription-Region": AZURE_TRANSLATOR_REGION,
        "Content-type": "application/json",
        "X-ClientTraceId": str(uuid.uuid4())
    }

    target_langs = ["ko", "en", "zh-Hans"]
    if src_lang in target_langs:
        target_langs.remove(src_lang)

    params = {
        "api-version": "3.0",
        "from": src_lang,
        "to": target_langs
    }

    body = [{"text": text}]
    res = http_requests.post(url, params=params, headers=headers, json=body)
    res.raise_for_status()
    translations = res.json()[0]["translations"]

    result = {src_lang: text}
    for item in translations:
        lang_key = item["to"]
        if lang_key == "zh-Hans":
            result["zh"] = item["text"]
        else:
            result[lang_key] = item["text"]

    for lang in ["ko", "en", "zh"]:
        result.setdefault(lang, "")

    return result

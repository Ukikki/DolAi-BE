import base64
import numpy as np
import uuid
import os
import asyncio
import re
from fastapi import FastAPI, WebSocket
from faster_whisper import WhisperModel
from datetime import datetime
from dotenv import load_dotenv
import requests
import webrtcvad

from hallucination_remover import HallucinationRemover

load_dotenv()

SPRING_URL = "https://3.34.92.187.nip.io/api/stt/log"
AZURE_TRANSLATOR_KEY = os.getenv("AZURE_TRANSLATOR_KEY")
AZURE_TRANSLATOR_REGION = os.getenv("AZURE_TRANSLATOR_REGION")
AZURE_TRANSLATOR_ENDPOINT = "https://api.cognitive.microsofttranslator.com"

# 환각 제거기 설정
remover = HallucinationRemover(
    stopwords=[
        # 일본어 환각
        "はい", "にんじん", "ににんが", "こんにちは", "ありがとう", "おはよう",
        "さようなら", "すみません", "ごめんなさい", "こんにちはよ",
        # 의미없는 영어 환각
        "little", "good.", "Oh,", "oh", "yeah", "yes", "okay",
        # 기타 환각
        "ㅎ", "핳", "시청자 여러분"
    ],
    allowed_languages=["korean", "english", "chinese"]
)

TODO_KEYWORDS = [
    "해줘", "해주세요", "해 주세요", "주세요", "오세요", "해 주삼", "해 줘", "해야 해", "완료해", "처리해", "해볼게", "할게요",
    "할 필요가 있어", "필요합니다", "조치", "진행", "정리해", "확인해", "해야겠어",
    "해야겠다", "일정", "마감", "하세요", "추가해", "등록해", "남겨", "정리",
    "assign", "need to", "must", "should", "complete", "submit", "handle", "todo"
]
BLOCK_PATTERNS = [
    "구독", "좋아요", "알림설정", "채널",
]

app = FastAPI()
vad = webrtcvad.Vad(2)
model = WhisperModel("small", device="cpu", compute_type="int8")

# 🎯 프롬프트 설정
WHISPER_PROMPT_AUTO = """Korean, Chinese, and English business meeting. The sentence may be cut off, do not make up words to fill in the rest of the sentence."""
WHISPER_PROMPT_KO = """Korean business meeting. Clear speech only."""

def fix_base64_padding(base64_str):
    padding = len(base64_str) % 4
    if padding != 0:
        base64_str += '=' * (4 - padding)
    return base64_str

def is_speech(audio_bytes: bytes, sample_rate=16000) -> bool:
    frame_duration = 30
    frame_size = int(sample_rate * frame_duration / 1000) * 2
    for i in range(0, len(audio_bytes), frame_size):
        frame = audio_bytes[i:i + frame_size]
        if len(frame) < frame_size:
            break
        if vad.is_speech(frame, sample_rate):
            return True
    return False

def contains_japanese_chars(text):
    """일본어 문자 포함 여부 강력 검증"""
    if not text:
        return False

    for char in text:
        char_code = ord(char)
        if ((0x3040 <= char_code <= 0x309F) or  # 히라가나
            (0x30A0 <= char_code <= 0x30FF) or  # 가타카나
            (0xFF66 <= char_code <= 0xFF9F)):   # 반각 가타카나
            return True

    return False

def calculate_text_quality(text):
    """텍스트 품질 점수 계산 (0-100)"""
    if not text:
        return 0

    quality_score = 0

    # 1. 길이 점수 (적당한 길이가 좋음)
    length = len(text.strip())
    if 5 <= length <= 100:
        quality_score += 30
    elif length > 100:
        quality_score += 20
    elif length >= 3:
        quality_score += 15

    # 2. 의미있는 문자 비율
    korean_chars = len([c for c in text if '\uAC00' <= c <= '\uD7A3'])
    english_chars = len([c for c in text if c.isalpha() and ord(c) < 256])
    chinese_chars = len([c for c in text if '\u4e00' <= c <= '\u9fff'])
    total_meaningful = korean_chars + english_chars + chinese_chars

    if len(text) > 0:
        meaningful_ratio = total_meaningful / len(text)
        quality_score += meaningful_ratio * 40

    # 3. 반복 패턴 감점
    if re.search(r'(.)\1{3,}', text):  # 같은 문자 4개 이상 반복
        quality_score -= 20

    # 4. 일본어 문자 감점
    if contains_japanese_chars(text):
        quality_score -= 50

    # 5. 특수문자만으로 구성 감점
    if re.match(r'^\W+$', text.strip()):
        quality_score -= 30

    return max(0, min(100, quality_score))

async def two_pass_transcribe(audio_np):
    """
    2-Pass STT: 1차 자동감지 → 일본어면 2차 한국어 강제

    Args:
        audio_np: 오디오 데이터

    Returns:
        tuple: (best_text, best_lang, pass_info)
    """

    # 🎯 1차 시도: 자동 언어 감지
    print("🔄 1차 STT 시도: 자동 언어 감지")
    try:
        segments_1st, info_1st = model.transcribe(
            audio_np,
            beam_size=1,
            vad_filter=True,
            temperature=0.0,
            initial_prompt=WHISPER_PROMPT_AUTO
        )

        first_text = ""
        for segment in segments_1st:
            if segment.text.strip():
                first_text = segment.text.strip()
                break

        detected_lang_1st = info_1st.language
        quality_1st = calculate_text_quality(first_text)

        print(f"   📊 1차 결과: '{first_text}' (언어: {detected_lang_1st}, 품질: {quality_1st})")

        # 🚨 일본어 감지 또는 일본어 문자 포함 시 2차 시도
        if (detected_lang_1st in ["ja", "japanese"] or
            contains_japanese_chars(first_text) or
            quality_1st < 30):  # 품질이 너무 낮아도 재시도

            print("🔄 2차 STT 시도: 한국어 강제 지정")

            try:
                segments_2nd, info_2nd = model.transcribe(
                    audio_np,
                    beam_size=1,
                    vad_filter=True,
                    temperature=0.0,
                    language="ko",  # 🔒 한국어 강제
                    initial_prompt=WHISPER_PROMPT_KO
                )

                second_text = ""
                for segment in segments_2nd:
                    if segment.text.strip():
                        second_text = segment.text.strip()
                        break

                detected_lang_2nd = info_2nd.language
                quality_2nd = calculate_text_quality(second_text)

                print(f"   📊 2차 결과: '{second_text}' (언어: {detected_lang_2nd}, 품질: {quality_2nd})")

                # 🎯 더 좋은 결과 선택
                if quality_2nd > quality_1st and not contains_japanese_chars(second_text):
                    print(f"   ✅ 2차 결과 채택 (품질 향상: {quality_1st} → {quality_2nd})")
                    return second_text, detected_lang_2nd, "2nd-pass-korean"
                else:
                    print(f"   ⚠️ 2차 결과도 불만족, 1차 결과 유지")

            except Exception as e:
                print(f"   ❌ 2차 STT 실패: {e}")

        # 1차 결과 사용
        return first_text, detected_lang_1st, "1st-pass-auto"

    except Exception as e:
        print(f"❌ 1차 STT 실패: {e}")
        return "", "unknown", "failed"

def detect_language_from_text(text):
    """텍스트 기반 언어 감지"""
    if not text:
        return "unknown"

    if contains_japanese_chars(text):
        return "ja"

    korean_chars = len([c for c in text if '\uAC00' <= c <= '\uD7A3' or '\u3131' <= c <= '\u318E'])
    english_chars = len([c for c in text if c.isalpha() and ord(c) < 256])
    chinese_chars = len([c for c in text if '\u4e00' <= c <= '\u9fff'])

    total_chars = korean_chars + english_chars + chinese_chars

    if total_chars == 0:
        return "unknown"

    korean_ratio = korean_chars / total_chars
    english_ratio = english_chars / total_chars
    chinese_ratio = chinese_chars / total_chars

    threshold = 0.3

    if korean_ratio >= threshold:
        return "ko"
    elif english_ratio >= threshold:
        return "en"
    elif chinese_ratio >= threshold:
        return "zh"
    else:
        return "unknown"

def check_language_validity(detected_lang, text):
    """언어 유효성 검사 - 일본어는 무조건 차단"""
    text_based_lang = detect_language_from_text(text)

    print(f"🔍 언어 분석: STT감지={detected_lang}, 텍스트분석={text_based_lang}")

    # 🚨 일본어는 무조건 차단
    if text_based_lang == "ja":
        print(f"🚫 일본어 텍스트 감지 → 완전 차단")
        return False, None

    if detected_lang == "ja" or detected_lang == "japanese":
        print(f"🚫 STT에서 일본어 감지 → 완전 차단")
        return False, None

    # 허용된 언어 처리
    if text_based_lang in ["ko", "en", "zh"]:
        return True, text_based_lang

    allowed_stt_languages = ["ko", "korean", "en", "english", "zh", "chinese", "zh-Hans"]
    if detected_lang in allowed_stt_languages:
        lang_mapping = {"korean": "ko", "english": "en", "chinese": "zh", "zh-Hans": "zh"}
        final_lang = lang_mapping.get(detected_lang, detected_lang)
        return True, final_lang

    return False, None

def is_valid_content(text):
    """유효한 자막 내용인지 검증"""
    if not text or len(text.strip()) < 2:
        return False

    if contains_japanese_chars(text):
        return False

    # 기본 유효성 검사들...
    quality = calculate_text_quality(text)
    return quality >= 30

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

            # Base64 디코딩
            audio_b64 = fix_base64_padding(audio_b64)
            audio_bytes = base64.b64decode(audio_b64)

            if len(audio_bytes) % 2 != 0:
                audio_bytes = audio_bytes[:-1]

            # 묵음 체크
            if not is_speech(audio_bytes):
                continue

            audio_np = np.frombuffer(audio_bytes, dtype=np.int16).astype(np.float32) / 32768.0

            # 🎯 2-Pass STT 실행
            text, detected_lang, pass_info = await two_pass_transcribe(audio_np)

            if not text:
                print("🚫 STT 결과 없음")
                continue

            print(f"🎤 STT 최종 결과: '{text}' (언어: {detected_lang}, 방식: {pass_info})")

            # 일본어 문자 포함 시 즉시 차단
            if contains_japanese_chars(text):
                print(f"🚫 일본어 문자 감지 → 자막 차단: '{text}'")
                continue

            # 언어 유효성 검사
            is_valid_lang, adjusted_lang = check_language_validity(detected_lang, text)

            if not is_valid_lang:
                print("🚫 허용되지 않은 언어 → 전송 차단!")
                continue

            # 환각 제거
            cleaned_text = remover.remove_hallucinations(text)
            if not cleaned_text:
                print("🚫 환각 제거 후 빈 텍스트")
                continue

            print(f"🧹 환각 제거 후: '{cleaned_text}'")

            # 환각 제거 후에도 일본어 문자 체크
            if contains_japanese_chars(cleaned_text):
                print(f"🚫 환각 제거 후에도 일본어 문자 감지 → 차단: '{cleaned_text}'")
                continue

            if any(re.search(pattern, cleaned_text, re.IGNORECASE) for pattern in BLOCK_PATTERNS):
                print(f"🚫 금지된 단어 포함 → 자막 차단: '{cleaned_text}'")
                continue

            # 중복 체크
            if cleaned_text == last_text:
                print(f"🚫 중복 텍스트 스킵: '{cleaned_text}'")
                continue

            # 유효성 검증
            if not is_valid_content(cleaned_text):
                print("🚫 유효성 검증 실패 → 전송 차단!")
                continue

            last_text = cleaned_text

            utterance_id = uuid.uuid4().hex
            segment_start = chunk_start_time
            timestamp = datetime.fromtimestamp(segment_start).strftime('%Y-%m-%dT%H:%M:%S')

            # 웹소켓 전송
            print(f"📱 웹소켓 전송: '{cleaned_text}' (방식: {pass_info})")
            # 자막 먼저 전송
            await websocket.send_json({"text": cleaned_text})

            # 번역 및 Spring 전송은 비동기로
            asyncio.create_task(translate_and_resend(
                cleaned_text, adjusted_lang, speaker, meeting_id, utterance_id, timestamp
            ))

            # 투두 감지도 비동기로 분리!
            if any(kw in cleaned_text.lower() for kw in TODO_KEYWORDS):
                print("📌 투두 감지됨! 백엔드 호출 예정")
                asyncio.create_task(send_todo_request(meeting_id))

    except Exception as e:
        print("❌ 연결 종료:", e)

async def send_todo_request(meeting_id):
    try:
        await asyncio.to_thread(requests.post, f"https://3.34.92.187.nip.io/api/llm/todo/extract/{meeting_id}", timeout=5)
        print("✅ 투두 요청 전송 완료")
    except Exception as e:
        print(f"⚠️ TODO 요청 실패: {e}")


async def translate_and_resend(text, lang, speaker, meeting_id, utterance_id, timestamp):
    """번역 및 Spring Boot 전송"""
    try:
        # 번역 전 최종 일본어 체크
        if contains_japanese_chars(text):
            print(f"🚫 번역 단계에서 일본어 문자 감지 → 전송 차단: '{text}'")
            return

        url = AZURE_TRANSLATOR_ENDPOINT + "/translate"
        headers = {
            "Ocp-Apim-Subscription-Key": AZURE_TRANSLATOR_KEY,
            "Ocp-Apim-Subscription-Region": AZURE_TRANSLATOR_REGION,
            "Content-type": "application/json"
        }

        target_langs = ["ko", "en", "zh-Hans"]
        if lang in target_langs:
            target_langs.remove(lang)

        params = {"api-version": "3.0", "from": lang, "to": target_langs}
        body = [{"text": text}]
        res = requests.post(url, params=params, headers=headers, json=body, timeout=10)
        res.raise_for_status()

        translations = res.json()[0]["translations"]
        result = {lang: text}
        for t in translations:
            if t["to"] == "zh-Hans":
                result["zh"] = t["text"]
            else:
                result[t["to"]] = t["text"]

        payload = {
            "meetingId": meeting_id, "speaker": speaker, "text": text,
            "textKo": result.get("ko", ""), "textEn": result.get("en", ""),
            "textZh": result.get("zh", ""), "timestamp": timestamp
        }

        print("📤 Spring 전송:", {k: v[:30] + "..." if len(str(v)) > 30 else v for k, v in payload.items()})
        requests.post(SPRING_URL, json=payload, timeout=10)
        print("✅ 번역 및 스프링 전송 완료")

    except Exception as e:
        print(f"❌ 번역 실패: {e}")
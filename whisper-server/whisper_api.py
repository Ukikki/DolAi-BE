# whispe_api.py
# 프론트/Spring이 HTTP로 요청하는 자막 API (테스트)
from flask import Flask, request, jsonify
from whisper_worker import transcribe_wav
from datetime import datetime
import requests
import os

app = Flask(__name__)

UPLOAD_FOLDER = "uploads"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
SPRING_URL = "http://localhost:8081/stt/log"

@app.route("/whisper/transcribe", methods=["POST"])
def transcribe():
    try:
        data = request.get_json()
        meeting_id = data.get("meetingId")
        speaker = data.get("speaker")
        audio_path = data.get("audioPath")

        if not (meeting_id and speaker and audio_path):
            return jsonify({"error": "Missing meetingId, speaker, or audioPath"}), 400

        text = transcribe_wav(audio_path)

        payload = {
            "meetingId": meeting_id,
            "speaker": speaker,
            "text": text,
            "timestamp": datetime.now().isoformat()
        }

        res = requests.post(SPRING_URL, json=payload)
        res.raise_for_status()

        return jsonify({"status": "ok", "transcribedText": text})

    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/whisper/upload-transcribe", methods=["POST"])
def upload_transcribe():
    try:
        if "file" not in request.files:
            return jsonify({"error": "Missing file in request"}), 400

        file = request.files["file"]
        meeting_id = request.form.get("meetingId")
        speaker = request.form.get("speaker")

        if not file or not meeting_id or not speaker:
            return jsonify({"error": "Missing required fields"}), 400

        filename = file.filename
        filepath = os.path.join(UPLOAD_FOLDER, filename)
        file.save(filepath)

        text = transcribe_wav(filepath)

        payload = {
            "meetingId": meeting_id,
            "speaker": speaker,
            "text": text,
            "timestamp": datetime.now().isoformat()
        }

        res = requests.post(SPRING_URL, json=payload)
        res.raise_for_status()

        return jsonify({"status": "ok", "transcribedText": text})

    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001, debug=True)

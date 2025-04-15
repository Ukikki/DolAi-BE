#whisper_api.py
from flask import Flask, request, jsonify
from whisper_worker import transcribe_from_pcm
from datetime import datetime
import requests
import base64

app = Flask(__name__)
SPRING_URL = "http://host.docker.internal:8081/stt/log"

@app.route("/whisper/stream", methods=["POST"])
def stream_transcribe():
    try:
        data = request.get_json()
        meeting_id = data.get("meetingId")
        speaker = data.get("speaker")
        audio_data_b64 = data.get("audioData")

        if not (meeting_id and speaker and audio_data_b64):
            return jsonify({"error": "Missing fields"}), 400

        audio_bytes = base64.b64decode(audio_data_b64)
        text = transcribe_from_pcm(audio_bytes, speaker, meeting_id)
        print("📥 Whisper 수신: audioData 길이 =", len(audio_bytes))

        return jsonify({"status": "ok", "transcribedText": text})

    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001, debug=True)

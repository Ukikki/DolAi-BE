# main.py
import os
import sys
import time
import argparse
import pyaudio
import webrtcvad
from queue import Queue
from whisper_worker import load_model, transcribe_audio

# 오디오 설정
SAMPLE_RATE = 16000
CHUNK_DURATION_MS = 20
CHUNK_SIZE = int(SAMPLE_RATE * CHUNK_DURATION_MS / 1000)
MIN_SPEECH_DURATION = 1.0
BUFFER_MAX_SIZE = 100
LOCK_FILE = "whisper.lock"

class AudioBuffer:
    def __init__(self):
        self.buffer = Queue(maxsize=BUFFER_MAX_SIZE)
        self.stop_recording = False

    def callback(self, in_data, frame_count, time_info, status):
        if not self.stop_recording:
            try:
                self.buffer.put(in_data)
            except:
                try:
                    self.buffer.get_nowait()
                    self.buffer.put(in_data)
                except:
                    pass
        return (None, pyaudio.paContinue)

    def get_chunk(self):
        try:
            return self.buffer.get_nowait()
        except:
            return None

def is_speech(frame, vad, sample_rate=SAMPLE_RATE):
    try:
        return vad.is_speech(frame, sample_rate)
    except:
        return False

def process_audio_buffer(audio_buffer, vad, model, max_duration=3):
    frames = []
    speech_detected = False
    speech_start_time = None
    total_duration = 0
    silence_duration = 0

    while total_duration < max_duration:
        chunk = audio_buffer.get_chunk()
        if chunk is None:
            time.sleep(0.01)
            continue

        total_duration += CHUNK_DURATION_MS / 1000

        if is_speech(chunk, vad):
            if not speech_detected:
                speech_detected = True
                speech_start_time = time.time()
            silence_duration = 0
            frames.append(chunk)
        else:
            if speech_detected:
                silence_duration += CHUNK_DURATION_MS / 1000
                frames.append(chunk)
                if silence_duration >= 1.5:
                    if (time.time() - speech_start_time) >= MIN_SPEECH_DURATION:
                        break

    if frames:
        transcribe_audio(frames, model)

def main():
    parser = argparse.ArgumentParser(description="Real-time captioning with VAD and Whisper.")
    parser.add_argument("-f", "--force", action="store_true", help="Force bypass the lock file check.")
    args = parser.parse_args()

    if os.path.exists(LOCK_FILE) and not args.force:
        print("Another instance is already running.")
        sys.exit(1)

    open(LOCK_FILE, "w").close()

    model = load_model()
    vad = webrtcvad.Vad(3)
    audio_buffer = AudioBuffer()
    p = pyaudio.PyAudio()
    stream = p.open(format=pyaudio.paInt16,
                    channels=1,
                    rate=SAMPLE_RATE,
                    input=True,
                    frames_per_buffer=CHUNK_SIZE,
                    stream_callback=audio_buffer.callback)

    stream.start_stream()
    print("🎤 실시간 자막 시작! 마이크에 대고 말해보세요.")

    try:
        while True:
            process_audio_buffer(audio_buffer, vad, model)
    except KeyboardInterrupt:
        print("\n⛔ 종료되었습니다.")
    except Exception as e:
        print(f"오류 발생: {e}")
    finally:
        audio_buffer.stop_recording = True
        try:
            stream.stop_stream()
        except Exception as e:
            print(f"[경고] 스트림 종료 중 에러: {e}")
        stream.close()
        p.terminate()
        if os.path.exists(LOCK_FILE):
            os.remove(LOCK_FILE)

if __name__ == "__main__":
    main()
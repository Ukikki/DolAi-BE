// server/whisper/ffmpegStream.js
import { spawn } from 'child_process';
import fetch from 'node-fetch';
import { EventEmitter } from 'events';

class FfmpegStream extends EventEmitter {
  constructor(rtpParameters, meetingId, speaker) {
    super();
    this.rtpParameters = rtpParameters;
    this.ffmpegProcess = null;
    this.audioQueue = [];
    this.queueSize = 0;
    this.processingInterval = null;
    this.isProcessing = false;
    this.targetSize = 48000; // 약 1.5초 분량 (16kHz, 16bit, mono)
    this.maxWaitTime = 3000; // 최대 대기 시간 (ms)
    this.lastProcessTime = Date.now();
    this.meetingId = meetingId;
    this.speaker = speaker;
    this._start();
  }

  _start() {
    const sdp = this._createSdp(this.rtpParameters);
    console.log("📄 [SDP 생성됨]\n" + sdp);

    this.ffmpegProcess = spawn('ffmpeg', [
      '-protocol_whitelist', 'file,pipe,udp,rtp',
      '-fflags', '+genpts',
      '-f', 'sdp',
      '-i', 'pipe:0',
      '-map', '0:a:0',
      '-acodec', 'pcm_s16le',
      '-ac', '1', // mono
      '-ar', '16000',
      '-af', 'aresample=async=1000',
      '-f', 's16le',
      'pipe:1'
    ]);

    this.ffmpegProcess.stdin.write(sdp);
    this.ffmpegProcess.stdin.end();

    this.ffmpegProcess.stdout.on('data', (chunk) => {
      this._enqueueAudio(chunk);
    });

    this.ffmpegProcess.stderr.on('data', (data) => {
      console.log('[FFmpeg stderr]', data.toString());
    });

    this.ffmpegProcess.on('close', (code) => {
      console.log(`FFmpeg 종료됨: 코드 ${code}`);
      clearInterval(this.processingInterval);
      this.emit('close');
    });

    // 주기적으로 큐 상태 확인 및 처리
    this.processingInterval = setInterval(() => this._checkQueue(), 500);
  }

  _enqueueAudio(chunk) {
    // 큐에 청크 추가
    this.audioQueue.push({
      data: chunk,
      timestamp: Date.now()
    });
    this.queueSize += chunk.length;

    console.log(`➕ 큐에 추가됨: ${chunk.length} bytes, 현재 큐 크기: ${this.queueSize} bytes`);

    // 큐가 목표 크기에 도달했는지 확인
    if (this.queueSize >= this.targetSize && !this.isProcessing) {
      this._processQueue();
    }
  }

  _checkQueue() {
    if (this.isProcessing || this.audioQueue.length === 0) return;

    const now = Date.now();
    const oldestChunk = this.audioQueue[0];
    const timeWaiting = now - oldestChunk.timestamp;

    // 오래 기다린 데이터가 있거나, 마지막 처리 후 일정 시간이 지났으면 처리
    if (timeWaiting >= this.maxWaitTime || (now - this.lastProcessTime >= this.maxWaitTime && this.queueSize > 0)) {
      console.log(`⏱️ 시간 기반 처리 트리거: ${timeWaiting}ms 대기, 큐 크기: ${this.queueSize} bytes`);
      this._processQueue();
    }
  }

  async _processQueue() {
    if (this.isProcessing || this.audioQueue.length === 0) return;

    this.isProcessing = true;

    try {
      // 모든 큐 데이터를 하나의 버퍼로 결합
      const chunks = this.audioQueue.map(item => item.data);
      const combinedBuffer = Buffer.concat(chunks);

      // 큐 초기화
      this.audioQueue = [];
      this.queueSize = 0;

      console.log(`🔄 큐 처리: ${combinedBuffer.length} bytes (약 ${(combinedBuffer.length/32000).toFixed(2)}초 오디오)`);

      if (combinedBuffer.length < 8000) {
        console.log('🔍 너무 짧은 오디오, 건너뜀');
        this.isProcessing = false;
        return;
      }

      // 디버깅을 위해 파일로 저장
      /*
      const fs = require('fs');
      const filename = `audio_chunk_${Date.now()}.raw`;
      fs.writeFileSync(filename, combinedBuffer);
      console.log(`💾 오디오 저장됨: ${filename}`);
      */

      console.log('🚀 Whisper로 전송 시작!');
      const response = await fetch('http://localhost:5001/whisper/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          meetingId: this.meetingId,
          speaker: this.speaker,
          audioData: combinedBuffer.toString('base64')
        })
      });

      if (!response.ok) {
        console.error(`Whisper API 오류: ${response.status}`);
      } else {
        console.log('✅ Whisper 전송 성공');
      }

      this.lastProcessTime = Date.now();
    } catch (err) {
      console.error('Whisper 전송 오류:', err);
    } finally {
      this.isProcessing = false;
    }
  }

  _createSdp({ ip, port, codec }) {
    return `v=0
o=- 0 0 IN IP4 ${ip}
s=WhisperAudio
c=IN IP4 ${ip}
t=0 0
m=audio ${port} RTP/AVP ${codec.payloadType}
a=rtcp:${port+1}
a=rtpmap:${codec.payloadType} ${codec.name}/${codec.clockRate}/${codec.channels || 2}
a=fmtp:${codec.payloadType} minptime=10;useinbandfec=1
`.replace(/\n/g, '\r\n');
  }

  stop() {
    if (this.ffmpegProcess) {
      clearInterval(this.processingInterval);
      this.ffmpegProcess.kill('SIGINT');
      this.ffmpegProcess = null;
    }
  }
}

export default FfmpegStream;
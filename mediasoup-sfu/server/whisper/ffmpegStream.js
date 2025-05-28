//ffmpegStream.js
import { spawn, execSync } from 'child_process';
import { EventEmitter } from 'events';
import WebSocket from 'ws';
import fs from 'fs';
import path from 'path';
import os from 'os';
import dgram from 'dgram';

function isSilence(buffer) {
  const threshold = 0.01;
  let sum = 0;
  for (let i = 0; i < buffer.length; i += 2) {
    const val = buffer.readInt16LE(i) / 32768.0;
    sum += val * val;
  }
  const rms = Math.sqrt(sum / (buffer.length / 2));
  return rms < threshold;
}

class FfmpegStream extends EventEmitter {
  constructor(rtpParameters, meetingId, speaker) {
    super();
    this.rtpParameters = rtpParameters;
    this.ffmpegProcess = null;
    this.audioQueue = [];
    this.queueSize = 0;
    this.processingInterval = null;
    this.isProcessing = false;
    this.targetSize = 64000; // 약 1.5초 분량 (16kHz, 16bit, mono)
    this.maxWaitTime = 3000; // 최대 대기 시간 (ms)
    this.lastProcessTime = Date.now();
    this.meetingId = meetingId;
    this.speaker = speaker;
    this.ws = null;
    this.sdpFilePath = null;
    this.processLock = false;

    // 🎯 오버래핑을 위한 버퍼들
    this.previousBuffer = null; // 이전 청크의 마지막 부분
    this.overlapSize = 16000; // 0.5초 오버랩 (16kHz * 0.5초 * 2bytes)
    this.maxOverlapSize = 32000; // 최대 1초 오버랩
    this.sentChunks = []; // 전송된 청크들의 해시 (중복 방지)
    this.maxSentChunksHistory = 10; // 최근 10개 청크 해시 저장

    console.log(`🎙️ FfmpegStream 생성됨 (오버래핑 방식):`, {
      meetingId,
      speaker,
      port: rtpParameters.port,
      codec: rtpParameters.codec.name,
      overlapSeconds: this.overlapSize / 32000
    });

    this.init();
    this._connectWebSocket();
  }

  async init() {
    await this._cleanupPort(this.rtpParameters.port);
    this._connectWebSocket();
  }

  // 포트 정리 메서드
  async _cleanupPort(port) {
    try {
      console.log(`🧹 포트 ${port} 정리 시도 중...`);

      if (process.platform === 'win32') {
        try {
          execSync(`for /f "tokens=5" %a in ('netstat -aon ^| findstr :${port}') do taskkill /F /PID %a`, { stdio: 'ignore' });
        } catch (e) {
          // 무시
        }
      } else {
        try {
          execSync(`lsof -i udp:${port} | grep -v PID | awk '{print $2}' | xargs -r kill -9 || true`, { stdio: 'ignore' });
        } catch (e) {
          // 무시
        }
      }

      console.log(`⏱️ 포트 ${port} 해제 대기 중... (500ms)`);
      await new Promise(resolve => setTimeout(resolve, 500));

    } catch (error) {
      console.error(`⚠️ 포트 정리 오류:`, error);
    }
  }

  // RTP 연결 테스트
  async _testRtpConnection(port) {
    return new Promise((resolve) => {
      try {
        const server = dgram.createSocket('udp4');

        console.log(`🔍 RTP 테스트: 포트 ${port}에서 패킷 리스닝 시작...`);

        const timeout = setTimeout(() => {
          server.close();
          console.warn(`⚠️ RTP 테스트: 3초 동안 패킷 없음, 포트 ${port}`);
          resolve(false);
        }, 3000);

        server.on('error', (err) => {
          clearTimeout(timeout);
          console.error(`❌ RTP 테스트 오류: ${err.message}`);
          server.close();
          resolve(false);
        });

        server.on('message', (msg, rinfo) => {
          clearTimeout(timeout);
          console.log(`✅ RTP 패킷 수신: ${msg.length} bytes, 출처=${rinfo.address}:${rinfo.port}`);
          server.close();
          resolve(true);
        });

        server.bind(port);

      } catch (err) {
        console.error(`❌ RTP 테스트 실패: ${err.message}`);
        resolve(false);
      }
    });
  }

  _connectWebSocket() {
    try {
      this.ws = new WebSocket('ws://127.0.0.1:5001/ws/whisper');

      this.ws.onopen = () => {
        console.log('🔌 WebSocket 연결됨');
        this._start();
      };

      this.ws.onerror = (err) => {
        console.error('WebSocket 오류:', err);
      };

      this.reconnectAttempts = this.reconnectAttempts || 0;

      this.ws.onclose = () => {
        this.reconnectAttempts++;
        if (this.reconnectAttempts > 5) {
          console.error('❌ WebSocket 재연결 5회 초과 → 중단');
          return;
        }

        const backoff = 1000 * this.reconnectAttempts;
        console.log(`🔁 WebSocket 재연결 시도 #${this.reconnectAttempts} (대기 ${backoff}ms)`);
        setTimeout(() => this._connectWebSocket(), backoff);
      };
    } catch (error) {
      console.error('WebSocket 연결 오류:', error);
      setTimeout(() => this._connectWebSocket(), 1000);
    }
  }

  // FFmpeg 시작 메서드
  async _start() {
    try {
      console.log(`🧪 RTP 연결 테스트 시작...`);
      const rtpAvailable = await this._testRtpConnection(this.rtpParameters.port);

      const tempDir = path.join(os.tmpdir(), 'ffmpeg-whisper');
      if (!fs.existsSync(tempDir)) {
        fs.mkdirSync(tempDir, { recursive: true });
      }

      const uniqueId = `${Date.now()}-${Math.floor(Math.random() * 10000)}`;
      this.sdpFilePath = path.join(tempDir, `whisper_${uniqueId}.sdp`);

      const sdp = this._createSdp(this.rtpParameters);
      fs.writeFileSync(this.sdpFilePath, sdp);
      console.log(`📄 SDP 파일 생성됨: ${this.sdpFilePath}`);

      const ffmpegArgs = [
        '-loglevel', 'error',
        '-protocol_whitelist', 'file,pipe,udp,rtp',
        '-rw_timeout', '30000000',
        '-analyzeduration', '10000000',
        '-probesize', '5000000',
        '-fflags', '+genpts+discardcorrupt+nobuffer',
        '-f', 'sdp',
        '-i', this.sdpFilePath,
        '-map', '0:a:0',
        '-acodec', 'pcm_s16le',
        '-ac', '1',
        '-ar', '16000',
        '-af', 'aresample=async=1000',
        '-f', 's16le',
        'pipe:1'
      ];

      console.log(`🚀 FFmpeg 실행: ffmpeg ${ffmpegArgs.join(' ')}`);

      const env = {
        ...process.env,
        FFREPORT: 'file=/tmp/ffmpeg-report.log:level=32',
      };

      this.ffmpegProcess = spawn('ffmpeg', ffmpegArgs, { env });

      this.ffmpegProcess.stdout.on('data', (chunk) => {
        this._enqueueAudio(chunk);
      });

      this.ffmpegProcess.stderr.on('data', (data) => {
        const text = data.toString().trim();
        if (text.toLowerCase().includes('error') && !text.includes('non-fatal')) {
          console.error('[FFmpeg ERROR]', text);
        }
      });

      this.ffmpegProcess.on('close', (code) => {
        console.log(`FFmpeg 종료됨 (코드 ${code})`);
        if (code !== 0) {
          console.error('⚠️ FFmpeg 비정상 종료!');
        }

        clearInterval(this.processingInterval);
        this._cleanupFiles();
        this.emit('close');
      });

      // 주기적으로 큐 상태 확인 및 처리
      this.processingInterval = setInterval(() => this._checkQueue(), 500);

    } catch (error) {
      console.error('FFmpeg 시작 오류:', error);
      this._cleanupFiles();
    }
  }

  _enqueueAudio(chunk) {
    if (isSilence(chunk)) {
      return;
    }

    this.audioQueue.push({
      data: chunk,
      timestamp: Date.now()
    });
    this.queueSize += chunk.length;

    console.log(`➕ Queue 추가: ${this.queueSize} bytes (큐길이: ${this.audioQueue.length})`);

    if (this.queueSize >= this.targetSize && !this.isProcessing && !this.processLock) {
      console.log(`🎯 목표 크기 도달 → 즉시 처리 시작`);
      this._processQueue();
    }
  }

  _checkQueue() {
    if (this.isProcessing || this.processLock || this.audioQueue.length === 0) {
      return;
    }

    const now = Date.now();
    const oldestChunk = this.audioQueue[0];
    const timeWaiting = now - oldestChunk.timestamp;

    const shouldProcess = (
        timeWaiting >= this.maxWaitTime ||
        (now - this.lastProcessTime >= this.maxWaitTime && this.queueSize > 0)
    );

    if (shouldProcess) {
      console.log(`⏱️ 시간 기반 처리 트리거: ${timeWaiting}ms 대기, 큐 크기: ${this.queueSize} bytes`);
      this._processQueue();
    }
  }

  // 🎯 오버래핑 청크 생성 함수
  _createOverlappingChunk(currentBuffer) {
    let finalBuffer = currentBuffer;

    // 🔄 1. 이전 청크의 마지막 부분을 앞에 붙이기
    if (this.previousBuffer && this.previousBuffer.length > 0) {
      // 이전 버퍼의 마지막 부분 추출
      const overlapStart = Math.max(0, this.previousBuffer.length - this.overlapSize);
      const previousOverlap = this.previousBuffer.slice(overlapStart);

      console.log(`🔗 이전 청크 오버랩 추가: ${previousOverlap.length} bytes (${(previousOverlap.length/32000).toFixed(2)}초)`);

      // 이전 오버랩 + 현재 버퍼 결합
      finalBuffer = Buffer.concat([previousOverlap, currentBuffer]);
    }

    // 🔄 2. 현재 버퍼를 다음을 위해 저장 (최대 크기 제한)
    if (currentBuffer.length > this.maxOverlapSize) {
      // 버퍼가 너무 크면 마지막 부분만 저장
      const saveStart = currentBuffer.length - this.maxOverlapSize;
      this.previousBuffer = currentBuffer.slice(saveStart);
    } else {
      this.previousBuffer = Buffer.from(currentBuffer); // 복사본 저장
    }

    return finalBuffer;
  }

  // 🎯 청크 중복 검사 (해시 기반)
  _generateChunkHash(buffer) {
    // 간단한 해시 생성 (처음, 중간, 끝 부분 샘플링)
    const start = buffer.readUInt32LE(0);
    const middle = buffer.length > 8 ? buffer.readUInt32LE(Math.floor(buffer.length / 2)) : 0;
    const end = buffer.length > 4 ? buffer.readUInt32LE(buffer.length - 4) : 0;

    return `${start}-${middle}-${end}-${buffer.length}`;
  }

  _isDuplicateChunk(buffer) {
    const hash = this._generateChunkHash(buffer);

    if (this.sentChunks.includes(hash)) {
      console.log(`🚫 중복 청크 감지 (해시: ${hash.slice(0, 20)}...)`);
      return true;
    }

    // 해시 히스토리에 추가
    this.sentChunks.push(hash);

    // 히스토리 크기 제한
    if (this.sentChunks.length > this.maxSentChunksHistory) {
      this.sentChunks.shift(); // 가장 오래된 것 제거
    }

    return false;
  }

  async _processQueue() {
    if (this.isProcessing || this.processLock) {
      console.log(`🚫 _processQueue 스킵: isProcessing=${this.isProcessing}, processLock=${this.processLock}`);
      return;
    }

    if (this.audioQueue.length === 0) {
      console.log(`🚫 _processQueue 스킵: 큐가 비어있음`);
      return;
    }

    this.processLock = true;
    this.isProcessing = true;

    try {
      // 큐 스냅샷 생성 후 즉시 초기화
      const queueSnapshot = [...this.audioQueue];
      const sizeSnapshot = this.queueSize;

      this.audioQueue = [];
      this.queueSize = 0;

      console.log(`🔄 큐 처리 시작: ${sizeSnapshot} bytes (${queueSnapshot.length} chunks)`);

      if (queueSnapshot.length === 0) {
        console.log('🚫 스냅샷이 비어있음, 처리 중단');
        return;
      }

      // 청크들을 하나의 버퍼로 결합
      const chunks = queueSnapshot.map(item => item.data);
      const currentBuffer = Buffer.concat(chunks);

      console.log(`📦 현재 청크: ${currentBuffer.length} bytes (약 ${(currentBuffer.length/32000).toFixed(2)}초)`);

      if (currentBuffer.length < 4000) {
        console.log('🔍 너무 짧은 오디오, 건너뜀');
        return;
      }

      // 🎯 오버래핑 청크 생성
      const overlappingBuffer = this._createOverlappingChunk(currentBuffer);

      console.log(`🔗 오버래핑 적용: ${currentBuffer.length} → ${overlappingBuffer.length} bytes (오버랩: ${overlappingBuffer.length - currentBuffer.length} bytes)`);

      // 🎯 중복 청크 검사
      if (this._isDuplicateChunk(overlappingBuffer)) {
        console.log('🚫 중복 청크 스킵');
        return;
      }

      console.log("📤 Whisper 전송 직전 확인:", {
        meetingId: this.meetingId,
        speaker: this.speaker,
        originalSize: currentBuffer.length,
        overlappedSize: overlappingBuffer.length,
        overlapRatio: ((overlappingBuffer.length - currentBuffer.length) / currentBuffer.length * 100).toFixed(1) + '%'
      });

      // WebSocket으로 전송
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({
          meetingId: this.meetingId,
          speaker: this.speaker,
          chunkStartTime: Date.now() / 1000,
          audioData: overlappingBuffer.toString('base64') // 오버래핑된 버퍼 전송
        }));
        console.log('✅ 오버래핑 청크 WebSocket 전송 완료');
      } else {
        console.error('❌ WebSocket이 열려 있지 않음!');

        // 전송 실패 시 데이터를 다시 큐에 넣기
        console.log('🔄 전송 실패 → 데이터를 큐 앞쪽에 다시 추가');
        this.audioQueue.unshift(...queueSnapshot);
        this.queueSize += sizeSnapshot;

        this._connectWebSocket();
      }

      this.lastProcessTime = Date.now();

    } catch (err) {
      console.error('❌ Whisper 전송 오류:', err);
    } finally {
      this.isProcessing = false;
      this.processLock = false;
      console.log(`✅ _processQueue 완료`);
    }
  }

  _createSdp({ ip, port, codec }) {
    const localIp = ip || '172.28.0.3';
    const payloadType = 100;

    const sdp = `v=0
o=- ${Date.now()} 1 IN IP4 ${localIp}
s=WhisperAudio
c=IN IP4 ${localIp}
t=0 0
m=audio ${port} RTP/AVP ${payloadType}
a=rtpmap:${payloadType} ${codec.name}/${codec.clockRate}/${codec.channels || 2}
a=recvonly
a=rtcp-mux
`.replace(/\n/g, '\r\n');

    console.log(`📄 [SDP 생성됨 - Payload Type ${payloadType}]:\n${sdp}`);
    return sdp;
  }

  _cleanupFiles() {
    if (this.sdpFilePath && fs.existsSync(this.sdpFilePath)) {
      try {
        fs.unlinkSync(this.sdpFilePath);
        console.log(`🧹 SDP 파일 삭제됨: ${this.sdpFilePath}`);
      } catch (e) {
        console.error('SDP 파일 삭제 오류:', e);
      }
      this.sdpFilePath = null;
    }
  }

  stop() {
    console.log('🛑 FfmpegStream 정리 중...');

    this.processLock = true;
    this.isProcessing = true;

    if (this.processingInterval) {
      clearInterval(this.processingInterval);
      this.processingInterval = null;
    }

    if (this.ffmpegProcess) {
      try {
        this.ffmpegProcess.kill('SIGINT');
      } catch (e) {
        console.error('FFmpeg 프로세스 종료 오류:', e);
      }
      this.ffmpegProcess = null;
    }

    if (this.ws) {
      try {
        this.ws.close();
      } catch (e) {
        console.error('WebSocket 종료 오류:', e);
      }
      this.ws = null;
    }

    this._cleanupFiles();

    // 🔧 오버래핑 관련 정리
    this.audioQueue = [];
    this.queueSize = 0;
    this.previousBuffer = null;
    this.sentChunks = [];

    console.log('🛑 FfmpegStream 정리 완료');
  }
}

export default FfmpegStream;
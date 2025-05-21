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

    console.log(`🎙️ FfmpegStream 생성됨:`, {
      meetingId,
      speaker,
      port: rtpParameters.port,
      codec: rtpParameters.codec.name
    });

    /*// 포트 충돌 문제 해결을 위해 포트 정리 먼저 수행
    this._cleanupPort(rtpParameters.port);*/

    // ❗ async 초기화는 여기서 직접 못 함
    this.init();  // 내부에서 await 사용 가능

    // 웹소켓 연결 및 FFmpeg 시작
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
        // Windows
        try {
          execSync(`for /f "tokens=5" %a in ('netstat -aon ^| findstr :${port}') do taskkill /F /PID %a`, { stdio: 'ignore' });
        } catch (e) {
          // 무시 - 프로세스가 없을 수 있음
        }
      } else {
        // macOS/Linux
        try {
          // UDP 포트 사용 프로세스 확인 및 종료
          execSync(`lsof -i udp:${port} | grep -v PID | awk '{print $2}' | xargs -r kill -9 || true`, { stdio: 'ignore' });
        } catch (e) {
          // 무시 - 프로세스가 없을 수 있음
        }
      }

      // 포트 해제될 시간 확보
      console.log(`⏱️ 포트 ${port} 해제 대기 중... (500ms)`);
      // EC2 터지는 원인 (1)
      /*const waitUntil = Date.now() + 500;
      while (Date.now() < waitUntil) {
        // 짧은 대기
      }*/
      await new Promise(resolve => setTimeout(resolve, 500)); // 안전: 비동기 sleep(Node.js 이벤트 루프 막지 않고 500ms 대기: CPU 사용량 0%에 가까움)

    } catch (error) {
      console.error(`⚠️ 포트 정리 오류:`, error);
    }
  }

  // RTP 연결 테스트
  async _testRtpConnection(port) {
    return new Promise((resolve, reject) => {
      try {
        const server = dgram.createSocket('udp4');

        console.log(`🔍 RTP 테스트: 포트 ${port}에서 패킷 리스닝 시작...`);

        // 3초 타임아웃
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
      this.ws = new WebSocket('ws://172.28.0.3:5001/ws/whisper');

      this.ws.onopen = () => {
        console.log('🔌 WebSocket 연결됨');
        // WebSocket 연결 후 FFmpeg 시작
        this._start();
      };

      this.ws.onerror = (err) => {
        console.error('WebSocket 오류:', err);
      };

      // EC2 터지는 원인 (2)
      /*this.ws.onclose = () => {
        console.log('🔌 WebSocket 연결 종료됨, 1초 후 재시도...');
        setTimeout(() => this._connectWebSocket(), 1000);
      };*/

      // 백오프 줘야함 // 수십 개의 FFmpegStream이 동시에 연결 시도할 때, 서버가 감당 못함
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

  // FFmpeg 시작 메서드 수정
  async _start() {
    try {
      // RTP 연결 테스트
      console.log(`🧪 RTP 연결 테스트 시작...`);
      const rtpAvailable = await this._testRtpConnection(this.rtpParameters.port);

      // 임시 디렉토리 준비
      const tempDir = path.join(os.tmpdir(), 'ffmpeg-whisper');
      if (!fs.existsSync(tempDir)) {
        fs.mkdirSync(tempDir, { recursive: true });
      }

      // 고유한 SDP 파일 생성 (숫자 정확히 처리)
      const uniqueId = `${Date.now()}-${Math.floor(Math.random() * 10000)}`;
      this.sdpFilePath = path.join(tempDir, `whisper_${uniqueId}.sdp`);

      // SDP 내용 생성 및 파일로 저장
      const sdp = this._createSdp(this.rtpParameters);
      fs.writeFileSync(this.sdpFilePath, sdp);
      console.log(`📄 SDP 파일 생성됨: ${this.sdpFilePath}`);

      // FFmpeg 명령어 개선 - loglevel 낮추고 옵션 간소화
      const ffmpegArgs = [
        '-loglevel', 'error', // ← 핵심 변경: debug → error
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

      // stdout 처리
      this.ffmpegProcess.stdout.on('data', (chunk) => {
        console.log(`📤 오디오 데이터 수신: ${chunk.length} bytes`);
        this._enqueueAudio(chunk);
      });

      // stderr 필터링 - 심각한 에러만 출력
      this.ffmpegProcess.stderr.on('data', (data) => {
        const text = data.toString().trim();
        if (text.toLowerCase().includes('error') && !text.includes('non-fatal')) {
          console.error('[FFmpeg ERROR]', text);
        }
      });

      // 종료 이벤트
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
    // 큐에 청크 추가
    this.audioQueue.push({
      data: chunk,
      timestamp: Date.now()
    });
    this.queueSize += chunk.length;

    console.log(`➕ Queue 추가 : ${this.queueSize} bytes`);

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

      if (combinedBuffer.length < 4000) {
        console.log('🔍 너무 짧은 오디오, 건너뜀');
        this.isProcessing = false;
        return;
      }

      console.log("📤 Whisper 전송 직전 확인:", {
        meetingId: this.meetingId,
        speaker: this.speaker,
      });

      // WebSocket으로 전송
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({
          meetingId: this.meetingId,
          speaker: this.speaker,
          chunkStartTime: Date.now() / 1000,
          audioData: combinedBuffer.toString('base64')
        }));
      } else {
        console.error('❌ WebSocket이 열려 있지 않음!');
        // WebSocket 재연결 시도
        this._connectWebSocket();
      }

      this.lastProcessTime = Date.now();
    } catch (err) {
      console.error('Whisper 전송 오류:', err);
    } finally {
      this.isProcessing = false;
    }
  }

  _createSdp({ ip, port, codec }) {
    // 127.0.0.1 대신 실제 PUBLIC_IP 사용
    const localIp = ip || '172.28.0.4';

    const sdp = `v=0
o=- ${Date.now()} 1 IN IP4 ${localIp}
s=WhisperAudio
c=IN IP4 ${localIp}
t=0 0
m=audio ${port} RTP/AVP ${codec.payloadType}
a=rtpmap:${codec.payloadType} ${codec.name}/${codec.clockRate}/${codec.channels || 2}
a=recvonly
a=rtcp-mux
`.replace(/\n/g, '\r\n');

    console.log(`📄 [SDP 생성됨]:\n${sdp}`);
    return sdp;
  }


  // 임시 파일 정리
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

  // 리소스 정리
  stop() {
    console.log('🛑 FfmpegStream 정리 중...');

    // 인터벌 정리
    if (this.processingInterval) {
      clearInterval(this.processingInterval);
      this.processingInterval = null;
    }

    // FFmpeg 프로세스 정리
    if (this.ffmpegProcess) {
      try {
        this.ffmpegProcess.kill('SIGINT');
      } catch (e) {
        console.error('FFmpeg 프로세스 종료 오류:', e);
      }
      this.ffmpegProcess = null;
    }

    // WebSocket 정리
    if (this.ws) {
      try {
        this.ws.close();
      } catch (e) {
        console.error('WebSocket 종료 오류:', e);
      }
      this.ws = null;
    }

    // 파일 정리
    this._cleanupFiles();

    console.log('🛑 FfmpegStream 정리 완료');
  }
}

export default FfmpegStream;
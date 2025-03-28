const express = require('express'); // Express 웹 서버 모듈
const http = require('http'); // HTTP 서버 모듈
const { Server } = require('ws'); // WebSocket 서버 모듈
const mediasoup = require('mediasoup'); // Mediasoup 라이브러리

const app = express();
const server = http.createServer(app);
const wss = new Server({ server }); // WebSocket 서버 생성

let worker, router;

// Mediasoup Worker 및 Router 초기화
(async () => {
    console.log("Starting Mediasoup Worker...");
    worker = await mediasoup.createWorker(); // Mediasoup Worker 생성
    router = await worker.createRouter({ // Router 생성 및 미디어 코덱 설정
        mediaCodecs: [
            { kind: 'audio', mimeType: 'audio/opus', clockRate: 48000, channels: 2 },
            { kind: 'video', mimeType: 'video/VP8', clockRate: 90000 }
        ]
    });
    console.log("Mediasoup Router Created!");
})();

// WebSocket 연결 처리
wss.on('connection', (ws) => {
    console.log('New WebSocket Connection');

    // 클라이언트에서 메시지를 받을 때 실행
    ws.on('message', async (message) => {
        const data = JSON.parse(message); // JSON 데이터 파싱

        // 클라이언트가 Router의 RTP Capabilities 요청
        if (data.type === 'getRouterRtpCapabilities') {
            ws.send(JSON.stringify({ type: 'routerRtpCapabilities', data: router.rtpCapabilities }));
        }
    });

    // 클라이언트 연결이 종료될 때 실행
    ws.on('close', () => {
        console.log('WebSocket Closed');
    });
});

// 서버 실행 및 포트 3001에서 대기
server.listen(3001, () => console.log('✅ Mediasoup SFU server running on port 3001'));

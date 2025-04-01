// server.js

const express = require('express');
const http = require('http');
const { Server } = require('ws');
const mediasoup = require('mediasoup');

const app = express();
const server = http.createServer(app);
const wss = new Server({ server });

app.use(express.static('public'));

const port = 3001;
server.listen(port, () => {
    console.log(`✅ SFU 서버 실행 중: http://localhost:${port}`);
});

// --- Mediasoup Worker 및 Router 초기화 ---
let worker, router;
let transports = [];
let producers = [];
let consumers = [];

(async () => {
    worker = await mediasoup.createWorker();
    router = await worker.createRouter({
        mediaCodecs: [
            {
                kind: 'audio',
                mimeType: 'audio/opus',
                clockRate: 48000,
                channels: 2,
            },
            {
                kind: 'video',
                mimeType: 'video/VP8',
                clockRate: 90000,
            },
        ],
    });
})();

// --- WebSocket Signaling 처리 ---
wss.on('connection', (socket) => {
    console.log('📡 WebSocket 연결됨');

    socket.on('message', async (msg) => {
        const data = JSON.parse(msg);

        switch (data.type) {
            case 'getRtpCapabilities':
                socket.send(JSON.stringify({ type: 'rtpCapabilities', data: router.rtpCapabilities }));
                break;

            case 'createTransport':
                const transport = await router.createWebRtcTransport({
                    listenIps: [{
                        ip: '0.0.0.0', // 서버 내부에서는 모든 IP에서 수신 가능
                        announcedIp: '9e5e-223-194-136-189.ngrok-free.app' // 클라이언트에게 알릴 외부 공인 IP
                    }],
                    enableUdp: true,
                    enableTcp: true,
                    preferUdp: true,
                });
                transports.push(transport);
                transport.observer.on('close', () => {
                    console.log('🚫 Transport closed');
                });

                socket.send(JSON.stringify({
                    type: 'transportCreated',
                    params: {
                        id: transport.id,
                        iceParameters: transport.iceParameters,
                        iceCandidates: transport.iceCandidates,
                        dtlsParameters: transport.dtlsParameters,
                    },
                }));

                socket.transport = transport;
                break;

            case 'connectTransport':
                await socket.transport.connect({ dtlsParameters: data.dtlsParameters });
                socket.send(JSON.stringify({ type: 'transportConnected' }));
                break;

            case 'produce':
                const producer = await socket.transport.produce({
                    kind: data.kind,
                    rtpParameters: data.rtpParameters,
                });
                producers.push(producer);
                socket.send(JSON.stringify({ type: 'produced', id: producer.id }));
                break;

            case 'consume':
                const consumer = await socket.transport.consume({
                    producerId: data.producerId,
                    rtpCapabilities: data.rtpCapabilities,
                    paused: false,
                });
                consumers.push(consumer);

                socket.send(JSON.stringify({
                    type: 'consumed',
                    params: {
                        id: consumer.id,
                        producerId: data.producerId,
                        kind: consumer.kind,
                        rtpParameters: consumer.rtpParameters,
                    },
                }));
                break;
        }
    });
});

const express = require('express');
const http = require('http');
const { Server } = require('ws');
const mediasoup = require('mediasoup');

const app = express();
const server = http.createServer(app);
const wss = new Server({ server });

let worker, router, producerTransport, consumerTransport, producer;

(async () => {
    console.log("Starting Mediasoup Worker...");
    worker = await mediasoup.createWorker();
    router = await worker.createRouter({
        mediaCodecs: [
            { kind: 'audio', mimeType: 'audio/opus', clockRate: 48000, channels: 2 },
            { kind: 'video', mimeType: 'video/VP8', clockRate: 90000 }
        ]
    });
    console.log("Mediasoup Router Created!");
})();

wss.on('connection', (ws) => {
    console.log('New WebSocket Connection');

    ws.on('message', async (message) => {
        const data = JSON.parse(message);

        if (data.type === 'getRouterRtpCapabilities') {
            ws.send(JSON.stringify({ type: 'routerRtpCapabilities', data: router.rtpCapabilities }));
        }

        // ProducerTransport 생성 요청
        else if (data.type === 'createProducerTransport') {
            producerTransport = await router.createWebRtcTransport({
                listenIps: [{ ip: '127.0.0.1', announcedIp: null }],
                enableUdp: true,
                enableTcp: true,
                preferUdp: true
            });

            ws.send(JSON.stringify({
                type: 'producerTransportCreated',
                params: {
                    id: producerTransport.id,
                    iceParameters: producerTransport.iceParameters,
                    iceCandidates: producerTransport.iceCandidates,
                    dtlsParameters: producerTransport.dtlsParameters
                }
            }));
        }

        // Producer 생성 요청
        else if (data.type === 'connectProducerTransport') {
            await producerTransport.connect({ dtlsParameters: data.dtlsParameters });

            producer = await producerTransport.produce({
                kind: data.kind,
                rtpParameters: data.rtpParameters
            });

            ws.send(JSON.stringify({ type: 'producerCreated', id: producer.id }));
        }

        // ConsumerTransport 생성 요청
        else if (data.type === 'createConsumerTransport') {
            consumerTransport = await router.createWebRtcTransport({
                listenIps: [{ ip: '127.0.0.1', announcedIp: null }],
                enableUdp: true,
                enableTcp: true,
                preferUdp: true
            });

            ws.send(JSON.stringify({
                type: 'consumerTransportCreated',
                params: {
                    id: consumerTransport.id,
                    iceParameters: consumerTransport.iceParameters,
                    iceCandidates: consumerTransport.iceCandidates,
                    dtlsParameters: consumerTransport.dtlsParameters
                }
            }));
        }

        // Consumer 생성 요청
        else if (data.type === 'connectConsumerTransport') {
            await consumerTransport.connect({ dtlsParameters: data.dtlsParameters });

            const consumer = await consumerTransport.consume({
                producerId: producer.id,
                rtpCapabilities: router.rtpCapabilities,
                paused: true
            });

            ws.send(JSON.stringify({
                type: 'consumerCreated',
                id: consumer.id,
                kind: consumer.kind,
                rtpParameters: consumer.rtpParameters
            }));
        }
    });

    ws.on('close', () => {
        console.log('WebSocket Closed');
    });
});

server.listen(3001, () => console.log('✅ Mediasoup SFU server running on port 3001'));

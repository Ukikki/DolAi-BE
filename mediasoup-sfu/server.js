const mediasoup = require("mediasoup");
const express = require("express");
const WebSocket = require("ws");

const app = express();
const server = app.listen(3000, '0.0.0.0',() => console.log("SFU Server running on port 3000"));
const wss = new WebSocket.Server({ server });

let worker, router;
const transports = new Map(); // 🔥 생성된 Transport를 저장할 객체

// Mediasoup Worker 생성
async function createMediasoupWorker() {
    worker = await mediasoup.createWorker();
    router = await worker.createRouter({
        mediaCodecs: [
            { kind: "audio", mimeType: "audio/opus", clockRate: 48000, channels: 2 },
            { kind: "video", mimeType: "video/VP8", clockRate: 90000 }
        ]
    });
    console.log("Mediasoup Worker & Router Created!");
}

createMediasoupWorker();

// WebSocket을 통한 Signaling
wss.on("connection", (ws) => {
    console.log("New WebSocket Connection");

    ws.on("message", async (message) => {
        try {
            console.log("Received message:", message.toString()); // 디버깅용 로그 추가
            const msg = JSON.parse(message);

            if (!msg.type) {
                console.error("Invalid message: Missing type field", msg);
                ws.send(JSON.stringify({ error: "Invalid message: Missing type" }));
                return;
            }

            // 🔹 클라이언트에서 createTransport 요청을 받으면 WebRTC Transport 생성
            if (msg.type === "createTransport") {
                const transport = await router.createWebRtcTransport({
                    listenIps: [{ ip: "0.0.0.0", announcedIp: "YOUR_PUBLIC_IP" }],
                    enableUdp: true,
                    enableTcp: true,
                });

                // Transport 저장
                transports.set(transport.id, transport);

                ws.send(JSON.stringify({
                    type: "transportCreated",
                    id: transport.id,
                    iceParameters: transport.iceParameters,
                    dtlsParameters: transport.dtlsParameters
                }));
            }

            // 🔹 클라이언트에서 connectTransport 요청을 받으면 Transport 연결
            if (msg.type === "connectTransport") {
                console.log("connectTransport 요청 수신:", msg);

                // Transport ID가 존재하는지 확인
                const transport = transports.get(msg.id);
                if (!transport) {
                    ws.send(JSON.stringify({ error: "Invalid Transport ID" }));
                    return;
                }

                // Transport 연결
                await transport.connect({ dtlsParameters: msg.dtlsParameters });

                ws.send(JSON.stringify({ type: "transportConnected", id: msg.id }));
            }
        } catch (error) {
            console.error("Error parsing JSON message:", error);
            ws.send(JSON.stringify({ error: "Invalid JSON format" }));
        }
    });
});

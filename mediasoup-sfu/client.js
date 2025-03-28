const WebSocket = require('ws');

// Mediasoup SFU WebSocket 서버에 연결
const ws = new WebSocket("ws://localhost:3001");

ws.on('open', () => {
    console.log("✅ WebSocket 연결 성공");

    // SFU에서 Router RTP Capabilities 요청
    ws.send(JSON.stringify({ type: "getRouterRtpCapabilities" }));
});

ws.on('message', (data) => {
    console.log("📩 SFU 응답:", data.toString());
});

ws.on('error', (error) => {
    console.error("❌ WebSocket 오류:", error);
});

ws.on('close', () => {
    console.log("🔌 WebSocket 연결 종료");
});

import * as mediasoupClient from 'mediasoup-client';

console.log("✅ script.js 파일이 로드되었습니다.");
console.log("📌 Mediasoup Client 로드됨:", mediasoupClient);

// WebSocket 연결 테스트
const ws = new WebSocket("ws://localhost:3001");

// URL에서 roomId 가져오기
const urlParams = new URLSearchParams(window.location.search);
const roomId = urlParams.get("roomId") || "defaultRoom";

const peerId = Math.random().toString(36).substr(2, 8);
let device;
let transport;
let producers = [];
let consumers = [];

ws.onopen = () => {
    console.log(`✅ WebSocket 연결 성공 (Room: ${roomId})`);
    ws.send(JSON.stringify({ type: "getRouterRtpCapabilities", roomId, peerId }));
};

ws.onmessage = async (message) => {
    console.log("📩 SFU 응답:", message.data);
};

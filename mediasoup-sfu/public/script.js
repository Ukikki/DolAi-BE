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

navigator.mediaDevices.getUserMedia({ video: true, audio: true })
    .then((stream) => {
        // 성공적으로 스트림을 가져왔을 때
        console.log("✅ 웹캠 스트리밍 시작!");
        document.getElementById("localVideo").srcObject = stream;  // 비디오 태그에 스트림 연결
    })
    .catch((err) => {
        // 실패할 경우
        console.error("❌ 웹캠 스트리밍 오류:", err);
        alert("카메라 또는 마이크에 접근할 수 없습니다. 권한을 확인해주세요.");
    });


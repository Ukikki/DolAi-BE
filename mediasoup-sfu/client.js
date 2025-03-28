const WebSocket = require('ws');
const ws = new WebSocket("ws://localhost:3001");

ws.on('open', () => {
    console.log("✅ WebSocket 연결 성공");

    // Router Capabilities 요청
    ws.send(JSON.stringify({ type: "getRouterRtpCapabilities" }));
});

ws.on('message', (message) => {
    const data = JSON.parse(message);
    console.log("📩 SFU 응답:", data);

    // ProducerTransport 생성 요청
    if (data.type === "routerRtpCapabilities") {
        ws.send(JSON.stringify({ type: "createProducerTransport" }));
    }

    // ProducerTransport 생성 완료 → Producer 연결 요청
    else if (data.type === "producerTransportCreated") {
        console.log("🎥 Producer Transport 생성 완료:", data.params.id);

        ws.send(JSON.stringify({
            type: "connectProducerTransport",
            dtlsParameters: data.params.dtlsParameters,
            kind: "video", // "audio" 도 가능
            rtpParameters: {
                codecs: [
                    {
                        mimeType: "video/VP8",
                        payloadType: 101,
                        clockRate: 90000,
                        rtcpFeedback: [],
                        parameters: {}
                    }
                ],
                encodings: [{ ssrc: 1111 }]
            }
        }));
    }

    // Producer 생성 완료 → ConsumerTransport 생성 요청
    else if (data.type === "producerCreated") {
        console.log("✅ Producer 생성 완료:", data.id);

        ws.send(JSON.stringify({ type: "createConsumerTransport" }));
    }

    // ConsumerTransport 생성 완료 → Consumer 연결 요청
    else if (data.type === "consumerTransportCreated") {
        console.log("🎥 Consumer Transport 생성 완료:", data.params.id);

        ws.send(JSON.stringify({
            type: "connectConsumerTransport",
            dtlsParameters: data.params.dtlsParameters
        }));
    }

    // Consumer 생성 완료 (이제 비디오/오디오 송출 가능)
    else if (data.type === "consumerCreated") {
        console.log("🎥 Consumer가 비디오를 수신합니다:", data);
    }
});

ws.on('error', (error) => {
    console.error("❌ WebSocket 오류:", error);
});

ws.on('close', () => {
    console.log("🔌 WebSocket 연결 종료");
});

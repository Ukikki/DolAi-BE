// public/client.js

import * as mediasoupClient from 'mediasoup-client';

const ws = new WebSocket('wss://9e5e-223-194-136-189.ngrok-free.app'); //  ngrok URL
let device, sendTransport, recvTransport;
let localStream;

ws.onmessage = async (message) => {
    const data = JSON.parse(message.data);

    switch (data.type) {
        case 'rtpCapabilities':
            await loadDevice(data.data);
            await createSendTransport();
            break;

        case 'transportCreated':
            if (!sendTransport) {
                createSendTransportFromServer(data.params);
            } else {
                createRecvTransportFromServer(data.params);
            }
            break;

        case 'transportConnected':
            break;

        case 'produced':
            // Produce 후 다른 참가자의 스트림 소비 요청
            ws.send(JSON.stringify({
                type: 'consume',
                rtpCapabilities: device.rtpCapabilities,
                producerId: data.id,
            }));
            break;

        case 'consumed':
            const consumer = await recvTransport.consume({
                id: data.params.id,
                producerId: data.params.producerId,
                kind: data.params.kind,
                rtpParameters: data.params.rtpParameters,
            });

            const remoteStream = new MediaStream();
            remoteStream.addTrack(consumer.track);
            document.getElementById('remoteVideo').srcObject = remoteStream;
            break;
    }
};

async function loadDevice(routerRtpCapabilities) {
    device = new mediasoupClient.Device();
    await device.load({ routerRtpCapabilities });
}

async function createSendTransport() {
    ws.send(JSON.stringify({ type: 'createTransport' }));
}

function createSendTransportFromServer(params) {
    sendTransport = device.createSendTransport(params);

    sendTransport.on('connect', ({ dtlsParameters }, callback) => {
        ws.send(JSON.stringify({ type: 'connectTransport', dtlsParameters }));
        callback();
    });

    sendTransport.on('produce', async ({ kind, rtpParameters }, callback) => {
        ws.send(JSON.stringify({ type: 'produce', kind, rtpParameters }));
        callback({ id: 'dummy' });
    });

    startStream();
}

function createRecvTransportFromServer(params) {
    recvTransport = device.createRecvTransport(params);

    recvTransport.on('connect', ({ dtlsParameters }, callback) => {
        ws.send(JSON.stringify({ type: 'connectTransport', dtlsParameters }));
        callback();
    });
}

async function startStream() {
    localStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: true });
    document.getElementById('localVideo').srcObject = localStream;

    const videoTrack = localStream.getVideoTracks()[0];
    await sendTransport.produce({ track: videoTrack });

    ws.send(JSON.stringify({ type: 'createTransport' }));
}

// 연결 시작
ws.onopen = () => {
    ws.send(JSON.stringify({ type: 'getRtpCapabilities' }));
};

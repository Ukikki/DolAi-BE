// WebRTC Transport 생성 및 관리
export async function createTransport(router) {
    const transport = await router.createWebRtcTransport({
        listenIps: [{ ip: '0.0.0.0', announcedIp: 'YOUR_PUBLIC_IP' }],
        enableUdp: true,
        enableTcp: true
    });
    return transport;
}
// Producer & Consumer 관리
export async function createProducer(transport, kind, rtpParameters) {
    const producer = await transport.produce({ kind, rtpParameters });
    return producer;
}

export async function createConsumer(router, transport, producerId) {
    const consumer = await transport.consume({
        producerId,
        rtpCapabilities: router.rtpCapabilities
    });
    return consumer;
}
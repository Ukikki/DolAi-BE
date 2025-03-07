// Worker 및 Router 관리
import mediasoup from 'mediasoup';

let worker;
let router;

export async function createWorker() {
    worker = await mediasoup.createWorker();
    router = await worker.createRouter({
        mediaCodecs: [
            { kind: 'audio', mimeType: 'audio/opus', clockRate: 48000, channels: 2 },
            { kind: 'video', mimeType: 'video/VP8', clockRate: 90000 }
        ]
    });
    return { worker, router };
}
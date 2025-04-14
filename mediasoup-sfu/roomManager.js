// roomManager.js
import * as mediasoup from 'mediasoup'

export const mediaCodecs = [
    {
        kind: 'audio',
        mimeType: 'audio/opus',
        clockRate: 48000,
        channels: 2,
    },
    {
        kind: 'video',
        mimeType: 'video/VP8',
        clockRate: 90000,
        parameters: {
            'x-google-start-bitrate': 1000,
        },
    },
]

export let worker = null
export const rooms = {}

export const createWorker = async () => {
    worker = await mediasoup.createWorker({
        rtcMinPort: 10000, // 넓은 범위로 설정
        rtcMaxPort: 20000,
    })
    console.log(`worker pid ${worker.pid}`)

    worker.on('died', error => {
        // This implies something serious happened, so kill the application
        console.error('mediasoup worker has died')
        setTimeout(() => process.exit(1), 2000) // exit in 2 seconds
    })

    return worker
}

export const createRoomIfNotExists = async (roomId) => {
    if (!rooms[roomId]) {
        const router = await worker.createRouter({ mediaCodecs })
        rooms[roomId] = {
            router,
            peers: [],
        }
        console.log(`✅ Room "${roomId}" created.`)
    }
}

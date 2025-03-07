import express from 'express';
import http from 'http';
import { Server } from 'socket.io';
import cors from 'cors'; // CORS 미들웨어 추가

const app = express();

// 🔹 CORS 설정 추가 (Express)
app.use(cors({
    origin: "*",   // 모든 도메인에서 요청 허용
    methods: ["GET", "POST"]
}));

const server = http.createServer(app);

const io = new Server(server, {
    cors: {
        origin: "*",  // 모든 출처에서 WebSocket 요청 허용
        methods: ["GET", "POST"]
    }
});

const rooms = new Map(); // 방 관리

io.on('connection', (socket) => {
    console.log(`✅ New WebSocket connection: ${socket.id}`);

    socket.on('createRoom', ({ roomId }) => {
        console.log(`📢 Received createRoom event: ${roomId}`);

        if (!roomId) {
            console.log(`⚠️ Room ID is missing!`);
            socket.emit('error', { message: 'Room ID is required' });
            return;
        }

        if (!rooms.has(roomId)) {
            rooms.set(roomId, { users: [] });
            console.log(`🏠 Room created: ${roomId}`);
            socket.join(roomId);
            io.to(roomId).emit('roomCreated', { roomId });
        } else {
            console.log(`⚠️ Room already exists: ${roomId}`);
            socket.emit('error', { message: 'Room already exists' });
        }
    });

    socket.on('disconnect', () => {
        console.log(`❌ Client disconnected: ${socket.id}`);
    });
});

// 🔹 CORS 설정된 WebSocket 서버 실행 (포트 3001)
server.listen(3001, () => console.log('🚀 Mediasoup SFU Server listening on port 3001'));

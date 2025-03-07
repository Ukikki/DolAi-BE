// WebSocket 메시지 핸들링
import { createRoom, joinRoom } from './roomManager.js';

export function handleWebSocketConnection(socket, io, worker) {
    socket.on('createRoom', async ({ roomId }) => {
        if (!roomId) return;
        const room = await createRoom(roomId, worker);
        socket.join(roomId);
        io.to(roomId).emit('roomCreated', { roomId });
    });

    socket.on('joinRoom', async ({ roomId }) => {
        if (!roomId) return;
        const room = joinRoom(roomId);
        if (!room) {
            socket.emit('error', { message: 'Room not found' });
            return;
        }
        socket.join(roomId);
        io.to(roomId).emit('newPeerJoined', { peerId: socket.id });
    });
}
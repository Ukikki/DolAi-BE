// 방(Room) 관리
const rooms = new Map();

export function createRoom(roomId, worker) {
    if (!rooms.has(roomId)) {
        rooms.set(roomId, { router: worker.router, peers: new Map() });
    }
    return rooms.get(roomId);
}

export function joinRoom(roomId) {
    return rooms.get(roomId);
}
// test-rtp.js
import dgram from 'dgram';

const port = parseInt(process.argv[2]) || 19998;
console.log(`🔍 RTP 패킷 모니터링: 포트 ${port}에서 리스닝 중...`);

const server = dgram.createSocket('udp4');

server.on('error', (err) => {
    console.error(`서버 오류:\n${err.stack}`);
    server.close();
});

let packetCount = 0;
let startTime = Date.now();

server.on('message', (msg, rinfo) => {
    packetCount++;

    if (packetCount === 1 || packetCount % 100 === 0) {
        console.log(`📦 패킷 #${packetCount}: ${rinfo.address}:${rinfo.port}에서 ${msg.length} 바이트 수신`);

        // RTP 헤더 분석 (기본 12바이트)
        if (msg.length >= 12) {
            const payloadType = msg[1] & 0x7f;
            const sequenceNumber = (msg[2] << 8) | msg[3];
            console.log(`  RTP 정보: 타입=${payloadType}, 시퀀스=${sequenceNumber}`);
        }

        const elapsed = (Date.now() - startTime) / 1000;
        console.log(`  초당 패킷 수: ${(packetCount / elapsed).toFixed(2)}`);
    }
});

server.on('listening', () => {
    console.log(`서버가 포트 ${port}에서 리스닝 중`);
});

server.bind(port);

// 60초 후 테스트 종료 및 결과 출력
setTimeout(() => {
    const elapsed = (Date.now() - startTime) / 1000;
    console.log(`\n📊 테스트 결과:`);
    console.log(`총 ${packetCount}개 패킷 수신 (${elapsed.toFixed(2)}초 동안)`);
    console.log(`평균 초당 패킷 수: ${(packetCount / elapsed).toFixed(2)}`);

    server.close();
    process.exit(0);
}, 60000);
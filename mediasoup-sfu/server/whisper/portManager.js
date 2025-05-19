import net from 'net';
import dgram from 'dgram';

class PortManager {
    constructor() {
        // 현재 사용 중인 포트 추적
        this.usedPorts = new Set();
        // 인스턴스와 포트 매핑
        this.instancePorts = new Map();
        // RTP 포트 범위 설정 (짝수 포트는 RTP, 홀수 포트는 RTCP용)
        this.minPort = 10000;
        this.maxPort = 20000;
        // 모니터링 인터벌
        this.monitoringInterval = null;
        // 초기화 로그
        console.log(`🚀 포트 매니저 초기화 완료 (포트 범위: ${this.minPort}-${this.maxPort})`);
    }

    /**
     * 포트가 현재 사용 중인지 확인
     * @param {number} port - 확인할 포트 번호
     * @returns {boolean} 포트 사용 여부
     */
    isPortInUse(port) {
        return this.usedPorts.has(port);
    }

    /**
     * 포트를 사용 중으로 등록
     * @param {string} instanceId - 인스턴스 식별자
     * @param {number} port - 사용할 포트 번호
     * @returns {boolean} 등록 성공 여부
     */
    registerPort(instanceId, port) {
        if (this.isPortInUse(port)) {
            return false;
        }

        // 포트와 포트+1 (RTP/RTCP 쌍) 등록
        this.usedPorts.add(port);
        this.usedPorts.add(port + 1);

        // 인스턴스와 포트 매핑
        if (!this.instancePorts.has(instanceId)) {
            this.instancePorts.set(instanceId, new Set());
        }
        this.instancePorts.get(instanceId).add(port);
        this.instancePorts.get(instanceId).add(port + 1);

        console.log(`📌 포트 ${port}/${port + 1} 등록됨 (인스턴스: ${instanceId})`);
        return true;
    }

    releaseInstancePorts(instanceId) {
        if (this.instancePorts.has(instanceId)) {
            const ports = this.instancePorts.get(instanceId);
            for (const port of ports) {
                this.usedPorts.delete(port);
            }
            console.log(`🧹 인스턴스 ${instanceId}의 모든 포트 해제됨: ${Array.from(ports).join(', ')}`);
            this.instancePorts.delete(instanceId);
        }
    }

    /**
     * 특정 포트 해제
     * @param {string} instanceId - 인스턴스 식별자
     * @param {number} port - 해제할 포트 번호
     */
    releasePort(instanceId, port) {
        this.usedPorts.delete(port);
        this.usedPorts.delete(port + 1);

        if (this.instancePorts.has(instanceId)) {
            const ports = this.instancePorts.get(instanceId);
            ports.delete(port);
            ports.delete(port + 1);
            console.log(`🧹 포트 ${port}/${port + 1} 해제됨 (인스턴스: ${instanceId})`);
        }
    }

    /**
     * 모든 포트 상태 출력
     */
    printPortStatus() {
        console.log(`\n📊 ===== 포트 매니저 상태 ===== 📊`);
        console.log(`🔢 사용 중인 포트 수: ${this.usedPorts.size}`);
        console.log(`👤 관리 중인 인스턴스 수: ${this.instancePorts.size}`);

        if (this.instancePorts.size > 0) {
            console.log(`\n📋 인스턴스별 포트 할당 현황:`);
            for (const [instanceId, ports] of this.instancePorts.entries()) {
                console.log(`  📌 ${instanceId}: ${Array.from(ports).sort((a, b) => a - b).join(', ')}`);
            }
        }
        console.log(`📊 ========================== 📊\n`);
    }

    /**
     * 주기적인 포트 상태 모니터링 시작
     * @param {number} interval - 모니터링 주기 (밀리초)
     */
    startMonitoring(interval = 60000) {
        if (this.monitoringInterval) {
            clearInterval(this.monitoringInterval);
        }

        this.monitoringInterval = setInterval(() => {
            this.printPortStatus();
        }, interval);

        console.log(`🔄 포트 상태 모니터링 시작 (간격: ${interval}ms)`);
    }

    /**
     * 포트 상태 모니터링 중지
     */
    stopMonitoring() {
        if (this.monitoringInterval) {
            clearInterval(this.monitoringInterval);
            this.monitoringInterval = null;
            console.log(`⏹️ 포트 상태 모니터링 중지됨`);
        }
    }

    /**
     * 모든 포트 강제 해제
     */
    releaseAllPorts() {
        console.log(`🧨 모든 포트 강제 해제 시작...`);
        const instanceIds = Array.from(this.instancePorts.keys());
        for (const instanceId of instanceIds) {
            this.releaseInstancePorts(instanceId);
        }
        this.usedPorts.clear();
        console.log(`🧨 모든 포트 강제 해제 완료 (총 ${instanceIds.length} 인스턴스)`);
    }

    /**
     * 시스템에서 사용 가능한 포트 가져오기
     * @returns {Promise<number>} 사용 가능한 포트 번호
     */
    async getAvailablePort() {
        return new Promise((resolve, reject) => {
            const server = net.createServer();

            // 타임아웃 설정
            const timeout = setTimeout(() => {
                try {
                    server.close();
                } catch (e) { /* 무시 */ }
                reject(new Error('포트 할당 타임아웃'));
            }, 2000);

            server.listen(0, () => {
                clearTimeout(timeout);
                const port = server.address().port;
                server.close(() => resolve(port));
            });

            server.on('error', (err) => {
                clearTimeout(timeout);
                reject(err);
            });
        });
    }

    /**
     * 특정 포트가 TCP 소켓으로 사용 가능한지 확인
     * @param {number} port - 확인할 포트 번호
     * @returns {Promise<boolean>} 포트 사용 가능 여부
     * @private
     */
    _checkTcpPortAvailable(port) {
        return new Promise((resolve, reject) => {
            const server = net.createServer();

            // 타임아웃 설정
            const timeout = setTimeout(() => {
                try {
                    server.close();
                } catch (e) { /* 무시 */ }
                reject(new Error(`TCP 포트 ${port} 확인 타임아웃`));
            }, 1000);

            server.once('error', (err) => {
                clearTimeout(timeout);
                if (err.code === 'EADDRINUSE') {
                    reject(new Error(`TCP 포트 ${port}가 이미 사용 중입니다.`));
                } else {
                    reject(err);
                }
            });

            server.once('listening', () => {
                clearTimeout(timeout);
                server.close(() => resolve(true));
            });

            server.listen(port);
        });
    }

    /**
     * 특정 포트가 UDP 소켓으로 사용 가능한지 확인
     * @param {number} port - 확인할 포트 번호
     * @returns {Promise<boolean>} 포트 사용 가능 여부
     * @private
     */
    _checkUdpPortAvailable(port) {
        return new Promise((resolve, reject) => {
            const server = dgram.createSocket('udp4');

            // 타임아웃 설정
            const timeout = setTimeout(() => {
                try {
                    server.close();
                } catch (e) { /* 무시 */ }
                reject(new Error(`UDP 포트 ${port} 확인 타임아웃`));
            }, 1000);

            server.on('error', (err) => {
                clearTimeout(timeout);
                if (err.code === 'EADDRINUSE') {
                    reject(new Error(`UDP 포트 ${port}가 이미 사용 중입니다.`));
                } else {
                    reject(err);
                }
            });

            server.bind(port, () => {
                clearTimeout(timeout);
                server.close(() => resolve(true));
            });
        });
    }

    /**
     * 포트 쌍이 사용 가능한지 확인 (TCP 및 UDP 모두)
     * @param {number} basePort - 확인할 기본 포트 번호
     * @returns {Promise<boolean>} 포트 쌍 사용 가능 여부
     * @private
     */
    async _checkPortPairAvailable(basePort) {
        try {
            // TCP 소켓 확인
            await this._checkTcpPortAvailable(basePort);
            await this._checkTcpPortAvailable(basePort + 1);

            // UDP 소켓 확인 (RTP/RTCP에 중요)
            await this._checkUdpPortAvailable(basePort);
            await this._checkUdpPortAvailable(basePort + 1);

            return true;
        } catch (err) {
            console.warn(`❌ 포트 ${basePort}/${basePort + 1} 사용 불가: ${err.message}`);
            return false;
        }
    }

    /**
     * 인스턴스 ID 기반으로 폴백 포트 생성 (비상용)
     * @param {string} instanceId - 인스턴스 식별자
     * @returns {number} 기본 포트 번호
     */
    getFallbackPort(instanceId) {
        // 인스턴스 ID를 해시하여 결정적인 포트 번호 생성
        const hash = instanceId.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
        const basePort = this.minPort + (hash % ((this.maxPort - this.minPort) / 2)) * 2; // 짝수 포트만

        console.warn(`⚠️ 폴백 포트 사용: ${instanceId} → ${basePort}/${basePort + 1}`);

        // 이미 사용 중이면 다른 포트 시도
        if (this.isPortInUse(basePort) || this.isPortInUse(basePort + 1)) {
            // 간단한 대체 알고리즘
            for (let offset = 2; offset < 100; offset += 2) {
                const altPort = basePort + offset;
                if (altPort < this.maxPort && !this.isPortInUse(altPort) && !this.isPortInUse(altPort + 1)) {
                    this.registerPort(instanceId, altPort);
                    return altPort;
                }
            }

            // 마지막 수단: 무작위 포트
            const randomPort = this.minPort + Math.floor(Math.random() * ((this.maxPort - this.minPort) / 2)) * 2;
            this.registerPort(instanceId, randomPort);
            return randomPort;
        }

        this.registerPort(instanceId, basePort);
        return basePort;
    }

    /**
     * RTP/RTCP 쌍으로 사용할 수 있는 포트 쌍 가져오기
     * @param {string} instanceId - 인스턴스 식별자
     * @returns {Promise<number>} RTP에 사용할 수 있는 짝수 포트
     */
    async getRtpPortPair(instanceId) {
        console.log(`🔍 인스턴스 ${instanceId}의 RTP 포트 쌍 할당 시작...`);

        // 최대 10번 시도
        for (let attempt = 0; attempt < 10; attempt++) {
            try {
                // 방법 1: 시스템이 제안하는 포트에서 시작
                const systemPort = await this.getAvailablePort();

                // 짝수 포트로 맞추기 (RTP는 짝수, RTCP는 홀수 포트 사용)
                let basePort = systemPort % 2 === 0 ? systemPort : systemPort + 1;

                // 범위 내로 조정
                if (basePort < this.minPort) basePort = this.minPort;
                if (basePort > this.maxPort - 1) basePort = this.maxPort - 2;

                // 방법 2: 범위 내에서 임의의 짝수 포트 시도
                if (attempt > 3) {
                    basePort = this.minPort + Math.floor(Math.random() * ((this.maxPort - this.minPort) / 2)) * 2;
                }

                console.log(`🔄 시도 ${attempt + 1}: 포트 ${basePort}/${basePort + 1} 가용성 확인 중...`);

                // 이미 내부적으로 사용 중인지 확인
                if (this.isPortInUse(basePort) || this.isPortInUse(basePort + 1)) {
                    console.log(`❌ 포트 ${basePort}/${basePort + 1}는 이미 내부에서 사용 중`);
                    continue;
                }

                // 이 포트가 시스템에서 사용 가능한지 확인
                const available = await this._checkPortPairAvailable(basePort);
                if (!available) {
                    continue;
                }

                // 포트 등록
                this.registerPort(instanceId, basePort);

                console.log(`✅ 인스턴스 ${instanceId}에 RTP 포트 ${basePort}/${basePort + 1} 할당 성공!`);
                return basePort;
            } catch (err) {
                console.error(`❌ 포트 할당 시도 ${attempt + 1} 실패:`, err.message);
            }
        }

        console.error(`❌ 모든 포트 할당 시도 실패. 폴백 포트 사용...`);
        // 모든 시도 실패 시 폴백 메커니즘 사용
        return this.getFallbackPort(instanceId);
    }
}

// 싱글톤 인스턴스
const portManager = new PortManager();

export default portManager;
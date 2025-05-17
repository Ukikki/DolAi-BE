import net from 'net';

/**
 * portManager.js포트 관리를 위한 싱글톤 클래스
 */
export class PortManager {
    constructor() {
        // 현재 사용 중인 포트 추적
        this.usedPorts = new Set();
        // 인스턴스와 포트 매핑
        this.instancePorts = new Map();
    }

    /**
     * 포트가
     * 현재 사용 중인지 확인
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

        return true;
    }

    /**
     * 인스턴스가 사용 중인 포트 해제
     * @param {string} instanceId - 인스턴스 식별자
     */
    releaseInstancePorts(instanceId) {
        if (this.instancePorts.has(instanceId)) {
            const ports = this.instancePorts.get(instanceId);
            for (const port of ports) {
                this.usedPorts.delete(port);
            }
            this.instancePorts.delete(instanceId);
            console.log(`🧹 인스턴스 ${instanceId}의 모든 포트 해제됨`);
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
            console.log(`🧹 포트 ${port}/${port+1} 해제됨 (인스턴스: ${instanceId})`);
        }
    }

    /**
     * 모든 포트 상태 출력
     */
    printPortStatus() {
        console.log(`📊 사용 중인 포트 수: ${this.usedPorts.size}`);
        console.log(`📊 관리 중인 인스턴스 수: ${this.instancePorts.size}`);
        for (const [instanceId, ports] of this.instancePorts.entries()) {
            console.log(`📌 인스턴스 ${instanceId}: ${Array.from(ports).join(', ')}`);
        }
    }

    /**
     * 시스템에서 사용 가능한 포트 가져오기
     * @returns {Promise<number>} 사용 가능한 포트 번호
     */
    async getAvailablePort() {
        return new Promise((resolve, reject) => {
            const server = net.createServer();
            server.listen(0, () => {
                const port = server.address().port;
                server.close(() => resolve(port));
            });
            server.on('error', reject);
        });
    }

    /**
     * RTP/RTCP 쌍으로 사용할 수 있는 포트 쌍 가져오기
     * @param {string} instanceId - 인스턴스 식별자
     * @returns {Promise<number>} RTP에 사용할 수 있는 짝수 포트
     */
    async getRtpPortPair(instanceId) {
        // 최대 10번 시도
        for (let attempt = 0; attempt < 10; attempt++) {
            try {
                // 첫 번째 사용 가능한 포트 찾기
                const port = await this.getAvailablePort();

                // 짝수 포트로 맞추기
                const basePort = port % 2 === 0 ? port : port + 1;

                // 이미 내부적으로 사용 중인지 확인
                if (this.isPortInUse(basePort) || this.isPortInUse(basePort + 1)) {
                    continue;
                }

                // 이 포트가 시스템에서 사용 가능한지 확인
                const available = await this._checkPortPairAvailable(basePort);
                if (!available) {
                    continue;
                }

                // 포트 등록
                this.registerPort(instanceId, basePort);

                return basePort;
            } catch (err) {
                console.error(`포트 할당 시도 ${attempt + 1} 실패:`, err.message);
            }
        }

        throw new Error('사용 가능한 RTP 포트 쌍을 찾을 수 없습니다.');
    }

    /**
     * 포트 쌍이 사용 가능한지 확인
     * @param {number} basePort - 확인할 기본 포트 번호
     * @returns {Promise<boolean>} 포트 쌍 사용 가능 여부
     * @private
     */
    async _checkPortPairAvailable(basePort) {
        try {
            await this._checkPortAvailable(basePort);
            await this._checkPortAvailable(basePort + 1);
            return true;
        } catch (err) {
            return false;
        }
    }

    /**
     * 특정 포트가 사용 가능한지 확인
     * @param {number} port - 확인할 포트 번호
     * @returns {Promise<boolean>} 포트 사용 가능 여부
     * @private
     */
    _checkPortAvailable(port) {
        return new Promise((resolve, reject) => {
            const server = net.createServer();
            server.once('error', (err) => {
                if (err.code === 'EADDRINUSE') {
                    reject(new Error(`포트 ${port}가 이미 사용 중입니다.`));
                } else {
                    reject(err);
                }
            });

            server.once('listening', () => {
                server.close(() => resolve(true));
            });

            server.listen(port);
        });
    }
}

// 싱글톤 인스턴스
const portManager = new PortManager();

export default portManager;
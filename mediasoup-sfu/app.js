/**
 * integrating mediasoup server with a node.js application
 */

const PUBLIC_IP = process.env.PUBLIC_IP || 'localhost';

/* Please follow mediasoup installation requirements */
/* https://mediasoup.org/documentation/v3/mediasoup/installation/ */
import express from 'express'
const app = express()

import https from 'httpolyglot'
import fs from 'fs'
import path from 'path'
const __dirname = path.resolve()

import { Server } from 'socket.io'
import mediasoup from 'mediasoup'

// rest.js 등록
import restRoutes from './routes/rest.js'

app.use(express.json()) // JSON 바디 파서 등록
app.use('/api', restRoutes)

// roomManager.js 등록
import { createWorker, rooms } from './roomManager.js'
import FfmpegStream from './server/whisper/ffmpegStream.js';
import portManager from './server/whisper/PortManager.js'

app.get('*', (req, res, next) => {
  const path = '/sfu/'

  if (req.path.indexOf(path) == 0 && req.path.length > path.length) return next()

  res.send(`You need to specify a room name in the path e.g. 'https://localhost/sfu/room'`)
})

app.use('/sfu/:room', express.static(path.join(__dirname, 'public')))

// SSL cert for HTTPS access
const options = {
  key: fs.readFileSync('./server/ssl/key.pem', 'utf-8'),
  cert: fs.readFileSync('./server/ssl/cert.pem', 'utf-8')
}

const httpsServer = https.createServer(options, app)
httpsServer.listen(3000, () => {
  console.log('listening on port: ' + 3000)
})

const io = new Server(httpsServer, {
  cors: { origin: '*' },
  transports: ['websocket'],
  pingInterval: 10000,
  pingTimeout: 30000,
})

// socket.io namespace (could represent a room?)
const connections = io.of('/mediasoup')

/**
 * Worker
 * |-> Router(s)
 *     |-> Producer Transport(s)
 *         |-> Producer
 *     |-> Consumer Transport(s)
 *         |-> Consumer 
 **/
let worker
//let rooms = {}          // { roomName1: { Router, rooms: [ sicketId1, ... ] }, ...}
let peers = {}          // { socketId1: { roomName1, socket, transports = [id1, id2,] }, producers = [id1, id2,] }, consumers = [id1, id2,], peerDetails }, ...}
let transports = []     // [ { socketId1, roomName1, transport, consumer }, ... ]
let producers = []      // [ { socketId1, roomName1, producer, }, ... ]
let consumers = []      // [ { socketId1, roomName1, consumer, }, ... ]

worker = await createWorker()

// This is an Array of RtpCapabilities
// https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities/#RtpCodecCapability
// list of media codecs supported by mediasoup ...
// https://github.com/versatica/mediasoup/blob/v3/src/supportedRtpCapabilities.ts
const mediaCodecs = [
  {
    kind: 'audio',
    mimeType: 'audio/opus',
    preferredPayloadType: 111,
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

// ffmpeg 포트 매니저
const buildFfmpegStream = async ({ router, codec, socketId, producerId, meetingId, userName }) => {
  const instanceId = `ffmpeg-${socketId}`;

  // 포트 할당
  const rtpPort = await portManager.getRtpPortPair(instanceId);
  console.log(`📡 [${instanceId}] 할당된 RTP 포트: ${rtpPort}/${rtpPort + 1}`);

  // plainTransport 생성 및 연결
  const plainTransport = await router.createPlainTransport({
    listenIp: { ip: '0.0.0.0', announcedIp: PUBLIC_IP },
    rtcpMux: false,
    comedia: true,
  });
  await plainTransport.connect({
    ip: '127.0.0.1',
    port: rtpPort,
    rtcpPort: rtpPort + 1,
  });
  console.log(`🔗 [${instanceId}] plainTransport 연결 완료`);

  // consumer 생성
  const consumer = await plainTransport.consume({
    producerId,
    rtpCapabilities: router.rtpCapabilities,
    paused: false,
  });
  await consumer.resume();
  console.log(`🎧 [${instanceId}] consumer 연결됨: ${consumer.id}`);

  // FFmpegStream 인스턴스 생성 및 시작
  const ffmpegStream = new FfmpegStream({
    ip: '127.0.0.1',
    port: rtpPort,
    codec: {
      name: codec.name,
      clockRate: codec.clockRate,
      payloadType: codec.payloadType,
    },
  }, meetingId, userName);

  console.log(`📼 [${instanceId}] FFmpegStream 준비 완료`);

  return { ffmpegStream, plainTransport };
};

connections.on('connection', async socket => {
  console.log('클라이언트 연결됨:', socket.id);

  const removeItems = (items, socketId, type) => {
    items.forEach(item => {
      if (item.socketId === socket.id) {
        item[type].close()
      }
    })
    items = items.filter(item => item.socketId !== socket.id)

    return items
  }

  socket.on('disconnect', (reason) => {
    // do some cleanup
    console.log('peer disconnected');
    console.log('서버와 연결 끊김 사유:', reason);

    consumers = removeItems(consumers, socket.id, 'consumer');
    producers = removeItems(producers, socket.id, 'producer');
    transports = removeItems(transports, socket.id, 'transport');

    if (!peers[socket.id]) {
      console.warn('Peer info not found on disconnect');
      return;
    }
    if (peers[socket.id]) {
      const oldSocket = peers[socket.id].socket;
      if (oldSocket.id !== socket.id) {
        oldSocket.disconnect(true); // 기존 소켓 강제 종료
      }
    }

    const { roomName } = peers[socket.id]
    delete peers[socket.id]

    // remove socket from room
    rooms[roomName] = {
      router: rooms[roomName].router,
      peers: rooms[roomName].peers.filter(socketId => socketId !== socket.id)
    }
  })

  socket.on('joinRoom',
      async ({ roomName, meetingId, userName, userId, rtpCapabilities }, callback) => {

        // ——— 동일 userId로 들어온 기존 세션 정리 ———
        for (const [oldSocketId, oldPeer] of Object.entries(peers)) {
          if (oldPeer.peerDetails.userId === userId) {
            console.log(`🔄 기존 세션 정리: userId=${userId} socketId=${oldSocketId}`);

            // producer 닫기
            oldPeer.producers?.forEach(producerId => {
              const found = producers.find(p => p.producer.id === producerId);
              found?.producer?.close();
            });

            // consumer 닫기
            oldPeer.consumers?.forEach(consumerId => {
              const found = consumers.find(c => c.consumer.id === consumerId);
              found?.consumer?.close();
            });

            // transport 닫기
            oldPeer.transports?.forEach(transportId => {
              const found = transports.find(t => t.transport.id === transportId);
              found?.transport?.close();
            });

            // 소켓 강제 종료
            oldPeer.socket.disconnect(true);

            // peers, producers, consumers, transports 배열 정리
            delete peers[oldSocketId];
            producers  = producers.filter(p => p.socketId !== oldSocketId);
            consumers  = consumers.filter(c => c.socketId !== oldSocketId);
            transports = transports.filter(t => t.socketId !== oldSocketId);
          }
        }

        // ——— 새 Peer 등록 ———
        const router1 = await createRoom(roomName, socket.id);
        peers[socket.id] = {
          socket,
          socketId: socket.id,
          roomName,
          meetingId,
          transports: [],
          producers: [],
          consumers: [],
          rtpCapabilities,
          peerDetails: {
            name: userName,
            userId,
            isAdmin: false,
          }
        };

        // **룸에 조인** 반드시 호출
        socket.join(roomName);

        console.log("📩 joinRoom 완료:", roomName, userName, userId);
        callback();
      }
  );

  const createRoom = async (roomName, socketId) => {
    // worker.createRouter(options)
    // options = { mediaCodecs, appData }
    // mediaCodecs -> defined above
    // appData -> custom application data - we are not supplying any
    // none of the two are required
    let router1
    let peers = []
    if (rooms[roomName]) {
      router1 = rooms[roomName].router
      peers = rooms[roomName].peers || []
    } else {
      router1 = await worker.createRouter({ mediaCodecs, })
    }

    console.log(`Router ID: ${router1.id}`, peers.length)

    rooms[roomName] = {
      router: router1,
      peers: [...peers, socketId],
    }

    return router1
  }

  // 비디오 rtp
  socket.on('getRouterRtpCapabilities', ({ roomName }, callback) => {
    const room = rooms[roomName];
    if (!room) return callback({ error: 'Room not found' }); // ← 여기 걸림
    const router = room.router;
    callback({ routerRtpCapabilities: router.rtpCapabilities });
  });

  socket.on('createWebRtcTransport', async ({ consumer }, callback) => {
    if (!peers[socket.id]) {
      console.warn(`⚠️ createWebRtcTransport: Peer not found for ${socket.id}`);
      return callback({ error: 'joinRoom을 먼저 호출해주세요.' });
    }
    // get Room Name from Peer's properties
    const roomName = peers[socket.id].roomName

    // get Router (Room) object this peer is in based on RoomName
    const router = rooms[roomName].router


    createWebRtcTransport(router).then(
        transport => {
          callback({
            params: {
              id: transport.id,
              iceParameters: transport.iceParameters,
              iceCandidates: transport.iceCandidates,
              dtlsParameters: transport.dtlsParameters,
            }
          })

          // add transport to Peer's properties
          addTransport(transport, roomName, consumer)
        },
        error => {
          console.log(error)
        })
  })

  // rtp 업데이터
  socket.on("updateRtpCapabilities", ({ roomName, rtpCapabilities }) => {
    if (peers[socket.id]) {
      peers[socket.id].rtpCapabilities = rtpCapabilities;
      console.log(`✅ RTP Capabilities 업데이트됨 for ${socket.id}`);
    }
  });

  const addTransport = (transport, roomName, consumer) => {
    if (!peers[socket.id]) {
      console.warn('addTransport 호출 시점에 peers에 없음:', socket.id);
      return;
    }

    transports = [
      ...transports,
      { socketId: socket.id, transport, roomName, consumer, }
    ];

    peers[socket.id] = {
      ...peers[socket.id],
      transports: [
        ...peers[socket.id].transports,
        transport.id,
      ]
    };
  };

  const addProducer = (producer, roomName) => {
    producers = [
      ...producers,
      { socketId: socket.id, producer, roomName, }
    ]

    peers[socket.id] = {
      ...peers[socket.id],
      producers: [
        ...peers[socket.id].producers,
        producer.id,
      ]
    }
  }

  const addConsumer = (consumer, roomName) => {
    // add the consumer to the consumers list
    consumers = [
      ...consumers,
      { socketId: socket.id, consumer, roomName, }
    ]

    // add the consumer id to the peers list
    peers[socket.id] = {
      ...peers[socket.id],
      consumers: [
        ...peers[socket.id].consumers,
        consumer.id,
      ]
    }
  }

  socket.on('getProducers', callback => {
    if (!peers[socket.id]) {
      console.warn(`⚠️ getProducers: Peer not found for ${socket.id}`);
      return callback([]);
    }

    const { roomName, peerDetails: { userId: myUserId } } = peers[socket.id];

    // video 프로듀서만, 같은 userId는 제외
    const producerList = producers
        // ① video 인 것만, ② 같은 userId(=내 것)는 빼고, ③ 같은 room
        .filter(p =>
            p.producer.kind === 'video' &&
            peers[p.socketId]?.peerDetails.userId !== myUserId &&
            p.roomName === roomName
        )
        .map(p => {
          const peer = peers[p.socketId];
          return {
            producerId: p.producer.id,
            peerId:     peer.peerDetails.userId,
            name:       peer.peerDetails.name || "익명"
          };
        });

    callback(producerList);
  });


  // 전역 캐시로 선언 (파일 상단 or connections.on 바깥)
  const informedCache = new Set(); // key: `${fromSocketId}_${toSocketId}_${producerId}`

  const informConsumers = (roomName, socketId, id, userId, kind) => {
    if (kind !== 'video') return;

    console.log(`🟡 informConsumers: new producer ${id} from ${socketId}`);

    producers.forEach(producerData => {
      const toSocketId = producerData.socketId;
      const isSameRoom = producerData.roomName === roomName;
      const isVideo = producerData.producer.kind === 'video';
      const isNotSelf = toSocketId !== socketId;

      const cacheKey = `${socketId}_${toSocketId}_${id}`;

      if (isSameRoom && isVideo && isNotSelf && !informedCache.has(cacheKey)) {
        informedCache.add(cacheKey);

        const producerSocket = peers[toSocketId].socket;
        const { peerDetails: { name } } = peers[socketId];

        producerSocket.emit('new-producer', {
          producerId: id,
          peerId:     userId,
          name:       name || "익명"
        });

        console.log(`✅ emit 'new-producer' → to ${toSocketId}`);
      }
    });
  };


  const getTransport = (socketId) => {
    const [producerTransport] = transports.filter(transport => transport.socketId === socketId && !transport.consumer)
    return producerTransport.transport
  }

  // see client's socket.emit('transport-connect', ...)
  socket.on("transport-connect", ({ dtlsParameters, transportId }) => {
    console.log("🔗 transport-connect 수신됨", transportId);

    const transportObj = transports.find(t => t.transport.id === transportId);
    if (transportObj) {
      transportObj.transport.connect({ dtlsParameters });
    } else {
      console.warn("존재하지 않는 transportId:", transportId);
    }
  });

  socket.on('transport-produce', async ({ kind, rtpParameters, appData }, callback) => {
    if (!peers[socket.id]) {
      console.warn(`⚠️ transport-produce: Peer not found for ${socket.id}`);
      return callback({ error: 'joinRoom을 먼저 호출해주세요.' });
    }

    if (rtpParameters.mid !== undefined) delete rtpParameters.mid;

    const { roomName } = peers[socket.id];
    const router = rooms[roomName].router;
    const peer = peers[socket.id];

    try {
      // 🎥 기존 videoProducer/screenProducer 정리 (mediaTag로 구분)
      if (kind === 'video') {
        const tag = appData?.mediaTag || 'video';
        if (tag === 'screen' && peer.screenProducer) {
          peer.screenProducer.close();
          delete peer.screenProducer;
        } else if (tag === 'camera' && peer.videoProducer) {
          peer.videoProducer.close();
          delete peer.videoProducer;
        }
      }

      const producer = await getTransport(socket.id).produce({
        kind,
        rtpParameters,
        appData,
        trace: true, // ✅ 필수
      });

      // 📡 trace 로그 찍기
      producer.on('trace', trace => {
        if (trace.type === 'rtp') {
          console.log(`📡 [Producer Trace] RTP: kind=${kind}, producerId=${producer.id}`);
        } else {
          console.log(`📡 [Producer Trace] 기타 이벤트:`, trace);
        }
      });

      // ✅ 등록
      addProducer(producer, roomName);

      if (kind === 'video') {
        const tag = appData?.mediaTag || 'camera';
        if (tag === 'screen') {
          peer.screenProducer = producer;
        } else {
          peer.videoProducer = producer;
        }

        producer.on('transportclose', () => {
          console.log(`🚪 video producer(${tag}) transport closed`);
          if (tag === 'screen') delete peer.screenProducer;
          else delete peer.videoProducer;
        });

        producer.on('trackended', () => {
          console.log(`📵 video track ended (${tag})`);
          producer.close();
          if (tag === 'screen') delete peer.screenProducer;
          else delete peer.videoProducer;
        });
      }

      if (kind === 'audio') {
        const codec = rtpParameters.codecs[0];
        const { meetingId, peerDetails: { name: userName } } = peer;

        const { ffmpegStream, plainTransport } = await buildFfmpegStream({
          router,
          codec: {
            name: codec.mimeType.split('/')[1],
            clockRate: codec.clockRate,
            payloadType: codec.payloadType,
          },
          socketId: socket.id,
          producerId: producer.id,
          meetingId,
          userName,
        });

        peer.audioProducer = producer;
        peer.ffmpeg = ffmpegStream;

        producer.on('transportclose', () => {
          console.log('🚪 audio producer transport closed');
          ffmpegStream.stop?.();
          delete peer.ffmpeg;
        });
      }

      const { userId } = peer.peerDetails;
      informConsumers(roomName, socket.id, producer.id, userId, kind);
      console.log('✅ Producer ID:', producer.id, kind);

      callback({ id: producer.id, producersExist: producers.length > 1 });

    } catch (err) {
      console.error('❌ transport-produce error:', err);
      callback({ error: err.message });
    }
  });

  // 마이크 상태 변경
  socket.on('audio-toggle', async ({ enabled }) => {
    const peer = peers[socket.id];
    const producer = peer?.audioProducer;
    const { roomName } = peer || {};

    if (!producer) return;

    if (enabled) {
      await producer.resume();
      console.log("🔊 마이크 재개됨");

      await new Promise(r => setTimeout(r, 300)); // 🔥 패킷 나오기까지 기다려줘야 함

      if (peer.ffmpeg) peer.ffmpeg.stop?.();
      const codec = producer.rtpParameters.codecs[0];
      const router = rooms[roomName].router;

      const { ffmpegStream } = await buildFfmpegStream({
        router,
        codec: {
          name: codec.mimeType.split('/')[1],
          clockRate: codec.clockRate,
          payloadType: codec.payloadType,
        },
        socketId: socket.id,
        producerId: producer.id,
        meetingId: peer.meetingId,
        userName: peer.peerDetails.name,
      });

      peer.ffmpeg = ffmpegStream;

      producer.on('transportclose', () => {
        ffmpegStream.stop?.();
        delete peer.ffmpeg;
      });

    } else {
      await producer.pause();
      peer.ffmpeg?.stop?.();
      delete peer.ffmpeg;
      console.log("🔕 마이크 OFF → ffmpeg 종료");
    }
  });

  // see client's socket.emit('transport-recv-connect', ...)
  socket.on('transport-recv-connect', async ({ dtlsParameters, serverConsumerTransportId }) => {
    console.log(`DTLS PARAMS: ${dtlsParameters}`)
    const consumerTransport = transports.find(transportData => (
        transportData.consumer && transportData.transport.id == serverConsumerTransportId
    )).transport
    await consumerTransport.connect({ dtlsParameters })
  })

  socket.on('consume', async ({ rtpCapabilities, remoteProducerId, serverConsumerTransportId }, callback) => {
    try {

      const { roomName } = peers[socket.id]
      const router = rooms[roomName].router
      let consumerTransport = transports.find(transportData => (
          transportData.consumer && transportData.transport.id == serverConsumerTransportId
      )).transport

      // check if the router can consume the specified producer
      if (router.canConsume({
        producerId: remoteProducerId,
        rtpCapabilities
      })) {
        // transport can now consume and return a consumer
        const consumer = await consumerTransport.consume({
          producerId: remoteProducerId,
          rtpCapabilities,
          paused: true,
        })

        consumer.on('transportclose', () => {
          console.log('transport close from consumer')
        })

        consumer.on('producerclose', () => {
          console.log('producer of consumer closed')
          socket.emit('producer-closed', { remoteProducerId })

          consumerTransport.close([])
          transports = transports.filter(transportData => transportData.transport.id !== consumerTransport.id)
          consumer.close()
          consumers = consumers.filter(consumerData => consumerData.consumer.id !== consumer.id)
        })

        addConsumer(consumer, roomName)

        // from the consumer extract the following params
        // to send back to the Client
        const params = {
          id: consumer.id,
          producerId: remoteProducerId,
          kind: consumer.kind,
          rtpParameters: consumer.rtpParameters,
          serverConsumerId: consumer.id,
        }

        // send the parameters to the client
        callback({ params })
      }
    } catch (error) {
      console.log(error.message)
      callback({
        params: {
          error: error
        }
      })
    }
  })

  socket.on('consumer-resume', async ({ serverConsumerId }) => {
    console.log('consumer resume')
    const { consumer } = consumers.find(consumerData => consumerData.consumer.id === serverConsumerId)
    await consumer.resume()
  })
})

const createWebRtcTransport = async (router) => {
  return new Promise(async (resolve, reject) => {
    try {
      // https://mediasoup.org/documentation/v3/mediasoup/api/#WebRtcTransportOptions
      const webRtcTransport_options = {
        listenIps: [
          {
            ip: '0.0.0.0', // replace with relevant IP address // 서버 내부용
            announcedIp: PUBLIC_IP, // 10.0.0.115 -> 맥북의 공인 IP(클라이언트에게 알려줄 공인 IP)
          }
        ],
        enableUdp: true,
        enableTcp: true,
        preferUdp: true,
      }

      // https://mediasoup.org/documentation/v3/mediasoup/api/#router-createWebRtcTransport
      let transport = await router.createWebRtcTransport(webRtcTransport_options)
      console.log(`transport id: ${transport.id}`)

      transport.on('dtlsstatechange', dtlsState => {
        if (dtlsState === 'closed') {
          transport.close()
        }
      })

      transport.on('close', () => {
        console.log('transport closed')
      })

      resolve(transport)

    } catch (error) {
      reject(error)
    }
  })
}

app.get('/sfu/:room', (req, res) => {
  const htmlPath = path.join(__dirname, 'public', 'index.html');
  let html = fs.readFileSync(htmlPath, 'utf-8');

  const PUBLIC_IP = process.env.PUBLIC_IP || '127.0.0.1';

  // IP 삽입 스크립트 추가
  html = html.replace(
      '</head>',
      `<script>window.__PUBLIC_IP__ = "${PUBLIC_IP}";</script></head>`
  );

  res.send(html);
});

// 정적 리소스(js, css 등)는 여전히 static으로 서비스
app.use('/sfu/:room', express.static(path.join(__dirname, 'public')));

package com.dolai.backend.meeting.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediasoupWebSocketClient {

    private static final String MEDIASOUP_SFU_URL = "ws://localhost:4443"; // Mediasoup WebSocket URL
    private StompSession stompSession;
    private final WebSocketStompClient stompClient;

    @PostConstruct
    public void connect() {
        try {
            StompSessionHandler sessionHandler = new CustomStompSessionHandler();
            stompSession = stompClient.connectAsync(MEDIASOUP_SFU_URL, sessionHandler).get();
            log.info("✅ Mediasoup WebSocket 연결 성공!");
        } catch (ExecutionException | InterruptedException e) {
            log.error("❌ Mediasoup WebSocket 연결 실패", e);
            Thread.currentThread().interrupt();
        }
    }

    public void createRouter(String meetingId) {
        sendMessage("{\"action\": \"createRouter\", \"meetingId\": \"" + meetingId + "\"}");
    }

    public void createTransport(String meetingId, String userId) {
        sendMessage("{\"action\": \"createTransport\", \"meetingId\": \"" + meetingId + "\", \"userId\": \"" + userId + "\"}");
    }

    public void sendMessage(String message) {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.send("/app/mediasoup", message);
            log.info("📩 Mediasoup에 메시지 전송: {}", message);
        } else {
            log.warn("⚠️ WebSocket 세션이 닫혀 있음. 메시지 전송 실패");
        }
    }

    @Component
    public static class WebSocketStompClientBean {
        @org.springframework.context.annotation.Bean
        public WebSocketStompClient stompClient() {
            WebSocketClient client = new StandardWebSocketClient();
            WebSocketStompClient stompClient = new WebSocketStompClient(client);
            stompClient.setMessageConverter(new org.springframework.messaging.converter.StringMessageConverter());
            return stompClient;
        }
    }

    private static class CustomStompSessionHandler extends StompSessionHandlerAdapter {
        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            log.info("✅ Stomp 세션 연결 완료: " + session.getSessionId());
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            log.info("📩 메시지 수신: " + payload);
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
            log.error("❌ Stomp 예외 발생", exception);
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            log.error("❌ Stomp 전송 오류 발생", exception);
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }
    }
}

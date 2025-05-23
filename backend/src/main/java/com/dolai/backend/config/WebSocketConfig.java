package com.dolai.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // STT
        registry.addEndpoint("/ws-stt")
                .setAllowedOrigins(
                        "https://74ca-113-198-83-192.ngrok-free.app",
                        "http://3.34.92.187.nip.io:5173",
                        "http://3.34.92.187:5173",
                        "http://localhost:5173"
                )
                .withSockJS();

        // 알림용
        registry.addEndpoint("/ws-notification")
                .setAllowedOrigins(
                        "https://74ca-113-198-83-192.ngrok-free.app",
                        "http://3.34.92.187.nip.io:5173",
                        "http://3.34.92.187:5173",
                        "http://localhost:5173"

                )
                .withSockJS();

        // 채팅
        registry.addEndpoint("/ws-chat")
                .setAllowedOrigins(
                        "https://74ca-113-198-83-192.ngrok-free.app",
                        "http://3.34.92.187.nip.io:5173",
                        "http://3.34.92.187:5173",
                        "http://localhost:5173"
                )
                .withSockJS();
    }
}
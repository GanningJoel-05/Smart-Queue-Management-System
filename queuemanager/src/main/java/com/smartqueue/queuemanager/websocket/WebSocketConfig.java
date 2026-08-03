package com.smartqueue.queuemanager.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Client subscribes to topics under /topic
        registry.enableSimpleBroker("/topic");
        // Client sends messages to server via /app prefix
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket handshake endpoint; SockJS fallback for non-WS browsers
        registry.addEndpoint("/ws-queue")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
package com.admin.server.framework.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @NonNull
    private final WebSocketHandler messageWebSocketHandler;
    @NonNull
    private final WebSocketHandshakeInterceptor handshakeInterceptor;
    private final String corsAllowedOrigins;

    public WebSocketConfig(@NonNull MessageWebSocketHandler messageWebSocketHandler,
                           @NonNull WebSocketHandshakeInterceptor handshakeInterceptor,
                           @Value("${app.cors.allowed-origins:*}") String corsAllowedOrigins) {
        this.messageWebSocketHandler = messageWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        String[] origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
        registry.addHandler(messageWebSocketHandler, "/ws/message")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns(origins.length > 0 ? origins : new String[]{"*"});
    }
}

package cn.rbac.server.framework.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MessageWebSocketHandler messageWebSocketHandler;
    private final WebSocketHandshakeInterceptor handshakeInterceptor;

    public WebSocketConfig(MessageWebSocketHandler messageWebSocketHandler,
                           WebSocketHandshakeInterceptor handshakeInterceptor) {
        this.messageWebSocketHandler = messageWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(messageWebSocketHandler, "/ws/message")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
    }
}

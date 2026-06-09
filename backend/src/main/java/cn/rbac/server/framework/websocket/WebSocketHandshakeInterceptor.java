package cn.rbac.server.framework.websocket;

import cn.rbac.server.framework.security.core.AuthTokenResolver;
import cn.rbac.server.framework.security.core.service.TokenService;
import jakarta.annotation.Resource;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手鉴权：从 Cookie / Header 读取 Token，不再使用 URL 查询参数。
 */
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private TokenService tokenService;

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        String token = AuthTokenResolver.resolve(servletRequest.getServletRequest());
        if (!StringUtils.hasText(token)) {
            return false;
        }
        Long userId = tokenService.getUserId(token);
        if (userId == null) {
            return false;
        }
        attributes.put("userId", userId);
        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler, @Nullable Exception exception) {
    }
}

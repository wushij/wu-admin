package com.admin.gateway.filter;

import com.admin.gateway.util.ClientIpUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 网关节点对 /api/auth 公开接口按 IP 限流，减轻刷接口与打到 RBAC 的压力（与后端限流叠加）。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class AuthEndpointRateLimitWebFilter implements WebFilter {

    private final ReactiveStringRedisTemplate redis;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gateway.security.rate-limit.login-per-minute:25}")
    private int loginPerMinute;

    @Value("${gateway.security.rate-limit.captcha-per-minute:35}")
    private int captchaPerMinute;

    @Value("${gateway.security.rate-limit.register-per-minute:8}")
    private int registerPerMinute;

    @Value("${gateway.security.rate-limit.config-per-minute:120}")
    private int configPerMinute;

    public AuthEndpointRateLimitWebFilter(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!path.startsWith("/api/auth")) {
            return chain.filter(exchange);
        }

        HttpMethod method = exchange.getRequest().getMethod();
        if (method == null) {
            return chain.filter(exchange);
        }

        int limit;
        String action;
        if (path.equals("/api/auth/login") && method == HttpMethod.POST) {
            limit = loginPerMinute;
            action = "login";
        } else if (path.equals("/api/auth/captcha") && method == HttpMethod.GET) {
            limit = captchaPerMinute;
            action = "captcha";
        } else if (path.equals("/api/auth/register") && method == HttpMethod.POST) {
            limit = registerPerMinute;
            action = "register";
        } else if (path.equals("/api/auth/config") && method == HttpMethod.GET) {
            limit = configPerMinute;
            action = "config";
        } else {
            return chain.filter(exchange);
        }

        if (limit <= 0) {
            return chain.filter(exchange);
        }

        String ip = ClientIpUtils.resolve(exchange.getRequest());
        long minute = System.currentTimeMillis() / 60_000L;
        String redisKey = "gw:rl:auth:" + action + ":" + ip + ":" + minute;

        return redis.opsForValue().increment(redisKey)
                .flatMap(n -> {
                    if (Long.valueOf(1).equals(n)) {
                        return redis.expire(redisKey, Duration.ofSeconds(90)).thenReturn(n);
                    }
                    return Mono.just(n);
                })
                .flatMap(n -> {
                    if (n != null && n > limit) {
                        return writeTooManyRequests(exchange);
                    }
                    return chain.filter(exchange);
                });
    }

    private Mono<Void> writeTooManyRequests(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("code", 429);
        body.put("msg", "请求过于频繁，请稍后再试");
        body.put("message", "请求过于频繁，请稍后再试");
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            bytes = "{\"code\":429,\"msg\":\"请求过于频繁，请稍后再试\"}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}

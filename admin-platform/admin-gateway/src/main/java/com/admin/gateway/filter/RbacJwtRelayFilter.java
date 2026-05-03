package com.admin.gateway.filter;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RbacJwtRelayFilter implements GlobalFilter, Ordered {

    private static final String SESSION_JWT_KEY = "rbacJwtToken";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!requiresJwtRelay(path)) {
            return chain.filter(exchange);
        }

        if (!StpUtil.isLogin()) {
            return chain.filter(exchange);
        }

        SaSession session = StpUtil.getSession();
        String rbacJwt = session.getString(SESSION_JWT_KEY);
        if (rbacJwt == null || rbacJwt.isEmpty()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.set("Authorization", "Bearer " + rbacJwt))
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    private boolean requiresJwtRelay(String path) {
        // StripPrefix 之后路径会从 /api/system/* 变成 /system/*，这里两种都兼容
        return path.startsWith("/api/system/")
                || path.startsWith("/api/dashboard/")
                || path.startsWith("/system/")
                || path.startsWith("/dashboard/")
                || path.startsWith("/rbac/");
    }

    @Override
    public int getOrder() {
        return 100;
    }
}

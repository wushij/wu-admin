package com.admin.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 为网关响应补充基础安全头（不替代 HTTPS / WAF）。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 20)
public class SecurityHeadersWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        exchange.getResponse().getHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponse().getHeaders().set("Referrer-Policy", "strict-origin-when-cross-origin");
        // Knife4j 需内嵌 iframe：勿与 RBAC 的 SAMEORIGIN 叠成 DENY+SAMEORIGIN
        if (!isKnife4jResource(path)) {
            exchange.getResponse().getHeaders().set("X-Frame-Options", "DENY");
        }
        return chain.filter(exchange);
    }

    private static boolean isKnife4jResource(String path) {
        if (path == null) {
            return false;
        }
        return "/api/doc.html".equals(path)
                || path.startsWith("/api/swagger-ui")
                || path.startsWith("/api/v3/api-docs")
                || path.startsWith("/api/webjars");
    }
}

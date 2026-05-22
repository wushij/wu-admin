package com.admin.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 将 URL 查询参数 Authorization 写入请求头，供 Sa-Token 校验。
 * 用于 img/video 等无法自定义 Header 的静态资源请求（/api/files/**）。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthorizationQueryWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (!request.getHeaders().containsKey("Authorization")) {
            String token = request.getQueryParams().getFirst("Authorization");
            if (StringUtils.hasText(token)) {
                ServerHttpRequest mutated = request.mutate()
                        .header("Authorization", token.trim())
                        .build();
                return chain.filter(exchange.mutate().request(mutated).build());
            }
        }
        return chain.filter(exchange);
    }
}

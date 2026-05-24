package com.admin.server.framework.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Knife4j 默认响应含 X-Frame-Options，导致管理端 iframe 空白；新窗口打开正常。
 * 开发环境 Vite 代理会删除该头，生产需在此允许同源嵌入。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class Knife4jIframeHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(request, response);
        if (isKnife4jDocPath(request)) {
            response.setHeader("X-Frame-Options", "SAMEORIGIN");
        }
    }

    private static boolean isKnife4jDocPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        return uri.endsWith("/doc.html")
                || uri.contains("/webjars/")
                || uri.contains("/swagger-ui")
                || uri.contains("/v3/api-docs");
    }
}

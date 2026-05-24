package com.admin.server.framework.web.filter;

import com.admin.server.framework.security.core.AuthTokenResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * 将 Cookie / URL 查询参数中的 Authorization 写入请求头，供 Sa-Token 校验。
 * 用于 img/video 等无法自定义 Header 的资源请求（同域 Cookie 优先）。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthorizationQueryFilter extends OncePerRequestFilter {

    private static boolean allowsTokenFallback(String path) {
        if (path == null) {
            return false;
        }
        return path.contains("/files/")
                || path.contains("/system/file/preview/")
                || path.contains("/system/file/download/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (allowsTokenFallback(path) && !StringUtils.hasText(request.getHeader(AuthTokenResolver.TOKEN_NAME))) {
            String token = AuthTokenResolver.resolve(request);
            if (!StringUtils.hasText(token)) {
                token = request.getParameter(AuthTokenResolver.TOKEN_NAME);
            }
            if (StringUtils.hasText(token)) {
                chain.doFilter(new AuthorizationHeaderRequestWrapper(request, token.trim()), response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static final class AuthorizationHeaderRequestWrapper extends HttpServletRequestWrapper {

        private final String authorization;

        AuthorizationHeaderRequestWrapper(HttpServletRequest request, String authorization) {
            super(request);
            this.authorization = authorization;
        }

        @Override
        public String getHeader(String name) {
            if (AuthTokenResolver.TOKEN_NAME.equalsIgnoreCase(name)) {
                return authorization;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (AuthTokenResolver.TOKEN_NAME.equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(authorization));
            }
            return super.getHeaders(name);
        }
    }
}

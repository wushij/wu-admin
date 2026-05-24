package com.admin.server.framework.security.core;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 从请求中解析 Sa-Token（Header 优先，其次 Cookie）。
 */
public final class AuthTokenResolver {

    public static final String TOKEN_NAME = "Authorization";

    private AuthTokenResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        return resolve(request, false);
    }

    /**
     * @param allowQueryParam 为 true 时在 Header/Cookie 缺失时回退读取 URL 查询参数（WebSocket 握手等场景）
     */
    public static String resolve(HttpServletRequest request, boolean allowQueryParam) {
        if (request == null) {
            return null;
        }
        String header = request.getHeader(TOKEN_NAME);
        if (StringUtils.hasText(header)) {
            return header.trim();
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (TOKEN_NAME.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue().trim();
                }
            }
        }
        if (allowQueryParam) {
            String query = request.getParameter(TOKEN_NAME);
            if (StringUtils.hasText(query)) {
                return query.trim();
            }
        }
        return null;
    }
}

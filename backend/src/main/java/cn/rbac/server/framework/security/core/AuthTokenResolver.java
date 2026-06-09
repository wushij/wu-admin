package cn.rbac.server.framework.security.core;

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
        return null;
    }
}

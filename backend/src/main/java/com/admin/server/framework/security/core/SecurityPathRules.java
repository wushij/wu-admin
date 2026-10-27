package com.admin.server.framework.security.core;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 与改造前 {@code SecurityConfig} 完全对齐的路径规则。
 * <p>匹配使用 {@code servletPath}（不含 context-path），与 Spring Security 的
 * {@code AntPathRequestMatcher} 语义一致；context-path 为 {@code /api}。
 */
public final class SecurityPathRules {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /** 等价于 {@code WebSecurityCustomizer.ignoring()}：完全绕过鉴权过滤器，也不记录在线活跃 */
    private static final List<String> IGNORED_PATHS = List.of(
            "/auth/**",
            "/api/auth/**",
            "/ws/**",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    );

    /** 等价于 {@code permitAll}：放行但仍执行过滤器（已登录时会刷新在线活跃时间） */
    private static final List<String> PERMIT_ALL_PATHS = List.of(
            "/pay/notify/**",
            // 蜜罐诱饵：必须匿名可达，否则扫描器只会拿到 401，陷阱永远不会被踩中。
            // 该前缀下不得挂载任何真实业务接口，详见 HoneypotPaths。
            HoneypotPaths.PREFIX_ANT
    );

    /** 等价于 {@code requestMatchers(HttpMethod.GET, "/files/**").permitAll()} */
    private static final List<String> PERMIT_ALL_GET_PATHS = List.of(
            "/files/**",
            "/api/storage/files/**",
            "/storage/files/**"
    );

    private SecurityPathRules() {
    }

    public static String resolvePath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (StringUtils.hasText(servletPath)) {
            return servletPath;
        }
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && uri != null && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri == null ? "" : uri;
    }

    public static boolean isIgnored(String path) {
        return matchesAny(IGNORED_PATHS, path);
    }

    public static boolean isPermitAll(String method, String path) {
        if ("GET".equalsIgnoreCase(method) && matchesAny(PERMIT_ALL_GET_PATHS, path)) {
            // 文件访问：只有 public/ 目录保持匿名可读，其余对象必须登录后走归属校验
            return isPublicStoragePath(path);
        }
        return matchesAny(PERMIT_ALL_PATHS, path);
    }

    /**
     * 判断文件路径是否位于可公开访问的 public/ 目录。
     * <p>兼容三种形态：{@code /files/public/...}、
     * {@code /storage/files/{bucket}/public/...}、{@code /api/storage/files/{bucket}/public/...}。</p>
     */
    private static boolean isPublicStoragePath(String path) {
        if (!StringUtils.hasText(path) || hasTraversalSegment(path)) {
            return false;
        }
        int storageIdx = path.indexOf("/storage/files/");
        if (storageIdx >= 0) {
            String rest = path.substring(storageIdx + "/storage/files/".length());
            int slash = rest.indexOf('/');
            String objectName = slash >= 0 ? rest.substring(slash + 1) : "";
            return objectName.startsWith("public/");
        }
        int filesIdx = path.indexOf("/files/");
        if (filesIdx >= 0) {
            return path.substring(filesIdx + "/files/".length()).startsWith("public/");
        }
        return false;
    }

    /**
     * 解码后按路径段判断是否包含 ..，防止 public/%2e%2e/files 之类的编码绕过。
     * 解码失败视为危险路径，直接拒绝匿名放行。
     */
    private static boolean hasTraversalSegment(String path) {
        String decoded;
        try {
            decoded = UriUtils.decode(path, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return true;
        }
        String normalized = decoded.replace('\\', '/');
        for (String segment : normalized.split("/")) {
            if ("..".equals(segment)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAny(List<String> patterns, String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        for (String pattern : patterns) {
            if (MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}

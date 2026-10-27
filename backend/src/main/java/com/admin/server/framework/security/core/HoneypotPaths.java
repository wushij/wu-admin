package com.admin.server.framework.security.core;

/**
 * 蜜罐路径集中定义。
 *
 * <p>所有诱饵路由必须落在 {@link #PREFIX} 之下。该前缀下**不得**再挂载任何真实业务接口，
 * 否则真实接口会被蜜罐处理器接管并返回伪造数据。</p>
 *
 * <p>注意 servletPath 与 requestURI 的差异：context-path 为 {@code /api}，
 * 过滤器链里拿到的是 {@code /system/internal/xxx}，而 requestURI 是
 * {@code /api/system/internal/xxx}，两者都需要匹配。</p>
 */
public final class HoneypotPaths {

    /** 蜜罐统一前缀（不含 context-path） */
    public static final String PREFIX = "/system/internal";

    /** 供 Spring MVC 与 AntPathMatcher 使用的通配形式 */
    public static final String PREFIX_ANT = PREFIX + "/**";

    /** 带 context-path 的前缀，用于按 requestURI 匹配 */
    public static final String PREFIX_WITH_CONTEXT = "/api" + PREFIX;

    private HoneypotPaths() {
    }

    /**
     * 判断路径是否属于蜜罐前缀。
     *
     * @param path servletPath 或 requestURI 均可
     * @return 是否命中蜜罐前缀
     */
    public static boolean matches(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return PREFIX.equals(path) || path.startsWith(PREFIX + "/")
                || PREFIX_WITH_CONTEXT.equals(path) || path.startsWith(PREFIX_WITH_CONTEXT + "/");
    }

    /**
     * 从请求路径中解析诱饵标识，例如 {@code /api/system/internal/db-backup} → {@code db-backup}。
     *
     * @param path 请求路径
     * @return 诱饵标识，无法解析时返回 {@code unknown}
     */
    public static String resolveTrapId(String path) {
        if (path == null || path.isEmpty()) {
            return "unknown";
        }
        String normalized = path;
        int idx = normalized.indexOf(PREFIX + "/");
        if (idx >= 0) {
            normalized = normalized.substring(idx + PREFIX.length() + 1);
        }
        int slash = normalized.indexOf('/');
        if (slash >= 0) {
            normalized = normalized.substring(0, slash);
        }
        return normalized.isEmpty() ? "unknown" : normalized;
    }
}

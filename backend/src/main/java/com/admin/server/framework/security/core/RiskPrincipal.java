package com.admin.server.framework.security.core;

import cn.hutool.core.util.StrUtil;

/**
 * 风险主体。
 *
 * <p>风控的计分和封禁都挂在主体上。当前支持两类：</p>
 * <ul>
 *   <li>{@code user:{id}} —— 已登录账号，登录用户换 IP 也能累计风险；</li>
 *   <li>{@code ip:{ip}} —— 真实客户端 IP，覆盖未登录请求，也是封禁实际生效的维度。</li>
 * </ul>
 *
 * <p>之所以保留 IP 作为封禁维度，是因为账号可以被换、设备标识可以被伪造，
 * 只有真实 IP 是过滤器链上最稳定的一个锚点。</p>
 *
 * @param key    Redis 中的主体标识，例如 {@code user:12}、{@code ip:1.2.3.4}
 * @param type   主体类型，{@code user} 或 {@code ip}
 * @param ip     真实客户端 IP，可能为空
 * @param userId 登录用户 ID，未登录时为 {@code null}
 */
public record RiskPrincipal(String key, String type, String ip, Long userId) {

    public static final String TYPE_USER = "user";
    public static final String TYPE_IP = "ip";

    /** 未知 IP 的占位主体，避免空串写进 Redis */
    private static final String UNKNOWN_IP = "unknown";

    public RiskPrincipal {
        if (StrUtil.isBlank(key)) {
            throw new IllegalArgumentException("risk principal key must not be blank");
        }
    }

    public static RiskPrincipal ofIp(String ip) {
        String normalizedIp = StrUtil.isBlank(ip) ? UNKNOWN_IP : ip.trim();
        return new RiskPrincipal(TYPE_IP + ":" + normalizedIp, TYPE_IP, normalizedIp, null);
    }

    public static RiskPrincipal ofUser(Long userId, String ip) {
        String normalizedIp = StrUtil.isBlank(ip) ? null : ip.trim();
        return new RiskPrincipal(TYPE_USER + ":" + userId, TYPE_USER, normalizedIp, userId);
    }

    /**
     * 兼容历史调用：接受裸 IP、{@code ip:} 前缀或 {@code user:} 前缀。
     *
     * <p>蜜罐与旧过滤器传入的是裸 IP，统一在这里补前缀，避免出现
     * 「写封禁的是裸 IP、读封禁的是带前缀 key」这种永远匹配不上的情况。</p>
     */
    public static RiskPrincipal normalize(String principal, String ip) {
        String raw = StrUtil.isBlank(principal) ? "" : principal.trim();
        if (raw.startsWith(TYPE_USER + ":")) {
            Long userId = parseId(raw.substring(TYPE_USER.length() + 1));
            String normalizedIp = StrUtil.isBlank(ip) ? null : ip.trim();
            return new RiskPrincipal(raw, TYPE_USER, normalizedIp, userId);
        }
        if (raw.startsWith(TYPE_IP + ":")) {
            return new RiskPrincipal(raw, TYPE_IP, raw.substring(TYPE_IP.length() + 1), null);
        }
        if (StrUtil.isNotBlank(raw)) {
            return ofIp(raw);
        }
        return ofIp(ip);
    }

    private static Long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean isUser() {
        return TYPE_USER.equals(type);
    }

    /** 运营面板展示用的短标识：用户显示 ID，IP 直接显示地址 */
    public String displayId() {
        if (isUser() && userId != null) {
            return String.valueOf(userId);
        }
        return StrUtil.blankToDefault(ip, key.substring(key.indexOf(':') + 1));
    }
}

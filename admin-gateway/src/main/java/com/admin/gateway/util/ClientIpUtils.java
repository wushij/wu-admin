package com.admin.gateway.util;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;

import java.net.InetAddress;

/**
 * 网关解析客户端 IP，与 RBAC {@code ClientIpUtils} 策略一致：X-Forwarded-For 取<strong>最右</strong>一段。
 */
public final class ClientIpUtils {

    private ClientIpUtils() {
    }

    public static String resolve(ServerHttpRequest request) {
        String xff = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            String[] parts = xff.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                String candidate = parts[i].trim();
                if (!candidate.isEmpty()) {
                    return stripIpv6Prefix(candidate);
                }
            }
        }
        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            int comma = realIp.indexOf(',');
            String first = comma > 0 ? realIp.substring(0, comma).trim() : realIp.trim();
            if (!first.isEmpty()) {
                return stripIpv6Prefix(first);
            }
        }
        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            return stripIpv6Prefix(request.getRemoteAddress().getAddress().getHostAddress());
        }
        return "unknown";
    }

    private static String stripIpv6Prefix(String ip) {
        if (ip == null || ip.isEmpty()) {
            return ip;
        }
        String trimmed = ip.trim();
        if (isLoopback(trimmed)) {
            return "127.0.0.1";
        }
        if (trimmed.startsWith("::ffff:") && trimmed.indexOf('.') > 0) {
            return trimmed.substring(7);
        }
        return trimmed;
    }

    private static boolean isLoopback(String ip) {
        try {
            return InetAddress.getByName(ip).isLoopbackAddress();
        } catch (Exception e) {
            return "127.0.0.1".equals(ip) || "::1".equals(ip);
        }
    }
}

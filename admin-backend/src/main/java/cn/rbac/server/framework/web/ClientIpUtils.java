package cn.rbac.server.framework.web;

import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;

/**
 * 解析真实客户端 IP。配合 Nginx {@code proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for} 时，
 * 逗号分隔列表中<strong>最右侧</strong>一般为直连代理的客户端地址，可避免左侧伪造段绕过限流。
 */
public final class ClientIpUtils {

    private ClientIpUtils() {
    }

    public static String resolve(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            String[] parts = xff.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                String candidate = parts[i].trim();
                if (!candidate.isEmpty()) {
                    return stripIpv6Prefix(candidate);
                }
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            int comma = realIp.indexOf(',');
            String first = comma > 0 ? realIp.substring(0, comma).trim() : realIp.trim();
            if (!first.isEmpty()) {
                return stripIpv6Prefix(first);
            }
        }
        String remote = request.getRemoteAddr();
        return StringUtils.hasText(remote) ? stripIpv6Prefix(remote) : "unknown";
    }

    /**
     * 规范化 IP：IPv4-mapped、IPv6 本机回环统一为 127.0.0.1，便于日志展示。
     */
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

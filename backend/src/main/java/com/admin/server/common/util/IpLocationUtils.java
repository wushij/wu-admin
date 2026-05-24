package com.admin.server.common.util;

import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

/**
 * IP 归属地解析（ip2region），供登录日志、在线用户等共用。
 */
@Slf4j
public final class IpLocationUtils {

    private static volatile Searcher searcher;

    private IpLocationUtils() {
    }

    public static String resolve(String ip) {
        if (!StringUtils.hasText(ip)) {
            return "未知";
        }
        String trimmed = ip.trim();
        if (isPrivateIp(trimmed)) {
            return "内网IP";
        }
        try {
            String region = getSearcher().search(trimmed);
            return formatRegion(region);
        } catch (Exception e) {
            log.warn("IP地址解析失败: {}, 错误: {}", trimmed, e.getMessage());
            return "未知";
        }
    }

    private static Searcher getSearcher() throws Exception {
        if (searcher == null) {
            synchronized (IpLocationUtils.class) {
                if (searcher == null) {
                    ClassPathResource resource = new ClassPathResource("ip2region/ip2region.xdb");
                    if (!resource.exists()) {
                        throw new IllegalStateException("IP2Region 数据库不存在: ip2region/ip2region.xdb");
                    }
                    byte[] dbBuff = resource.getInputStream().readAllBytes();
                    searcher = Searcher.newWithBuffer(dbBuff);
                    log.info("IP2Region 数据库已加载，大小 {} bytes", dbBuff.length);
                }
            }
        }
        return searcher;
    }

    static boolean isPrivateIp(String ip) {
        return ip.startsWith("127.")
                || ip.startsWith("192.168.")
                || ip.startsWith("10.")
                || ip.startsWith("172.16.")
                || ip.startsWith("172.17.")
                || ip.startsWith("172.18.")
                || ip.startsWith("172.19.")
                || ip.startsWith("172.20.")
                || ip.startsWith("172.21.")
                || ip.startsWith("172.22.")
                || ip.startsWith("172.23.")
                || ip.startsWith("172.24.")
                || ip.startsWith("172.25.")
                || ip.startsWith("172.26.")
                || ip.startsWith("172.27.")
                || ip.startsWith("172.28.")
                || ip.startsWith("172.29.")
                || ip.startsWith("172.30.")
                || ip.startsWith("172.31.")
                || "0:0:0:0:0:0:0:1".equals(ip)
                || "::1".equals(ip);
    }

    /**
     * 中国|0|江苏省|苏州市|电信 → 江苏苏州(电信)
     */
    static String formatRegion(String region) {
        if (!StringUtils.hasText(region)) {
            return "未知";
        }
        String[] parts = region.split("\\|");
        StringBuilder location = new StringBuilder();

        if (parts.length >= 5) {
            if (!"0".equals(parts[2])) {
                location.append(normalizePlace(parts[2]));
            }
            if (!"0".equals(parts[3])) {
                location.append(normalizePlace(parts[3]));
            }
            if (!"0".equals(parts[4])) {
                location.append("(").append(parts[4]).append(")");
            }
        } else if (parts.length >= 3) {
            location.append(normalizePlace(parts[parts.length - 2]));
            location.append(normalizePlace(parts[parts.length - 1]));
        }

        return location.length() > 0 ? location.toString() : "未知";
    }

    private static String normalizePlace(String part) {
        if (!StringUtils.hasText(part) || "0".equals(part)) {
            return "";
        }
        return part.replace("省", "")
                .replace("自治区", "")
                .replace("市", "");
    }
}

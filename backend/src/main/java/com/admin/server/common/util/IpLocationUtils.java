package com.admin.server.common.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * IP 归属地解析（IPv4 使用 ip2region 离线库，IPv6 使用在线查询 + 本地缓存），供登录日志、在线用户等共用。
 */
@Slf4j
public final class IpLocationUtils {

    private static volatile Searcher searcher;

    /**
     * IPv6 归属地本地 LRU 缓存（上限 2048 条，线程安全）
     */
    private static final Map<String, String> IPV6_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > 2048;
                }
            }
    );

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
        // IPv6 独立分支解析（ip2region 离线库仅支持 IPv4）
        if (isIpv6(trimmed)) {
            return resolveIpv6(trimmed);
        }
        // IPv4 使用 ip2region 离线库查询
        try {
            String region = getSearcher().search(trimmed);
            return formatRegion(region);
        } catch (Exception e) {
            log.warn("IPv4地址解析失败: {}, 错误: {}", trimmed, e.getMessage());
            return "未知";
        }
    }

    /**
     * 判断是否为 IPv6 地址
     */
    public static boolean isIpv6(String ip) {
        return ip != null && ip.indexOf(':') >= 0;
    }

    /**
     * 解析 IPv6 归属地（带内存缓存与快速超时降级）
     */
    private static String resolveIpv6(String ipv6) {
        String cached = IPV6_CACHE.get(ipv6);
        if (cached != null) {
            return cached;
        }
        try {
            // 超时时间控制在 1500ms，避免登录等核心链路受阻
            String url = "http://ip-api.com/json/" + ipv6 + "?lang=zh-CN";
            String resp = HttpUtil.createGet(url)
                    .timeout(1500)
                    .header("User-Agent", "Mozilla/5.0")
                    .execute()
                    .body();
            if (StrUtil.isNotBlank(resp)) {
                JSONObject json = JSONUtil.parseObj(resp);
                if ("success".equals(json.getStr("status"))) {
                    String country = json.getStr("country", "");
                    String regionName = json.getStr("regionName", "");
                    String city = json.getStr("city", "");
                    String isp = json.getStr("isp", "");

                    StringBuilder sb = new StringBuilder();
                    if ("中国".equals(country)) {
                        sb.append(normalizePlace(regionName));
                        if (!regionName.equals(city) && StrUtil.isNotBlank(city)) {
                            sb.append(normalizePlace(city));
                        }
                    } else {
                        sb.append(country);
                        if (StrUtil.isNotBlank(regionName)) {
                            sb.append(normalizePlace(regionName));
                        }
                        if (StrUtil.isNotBlank(city) && !city.equals(regionName)) {
                            sb.append("(").append(city).append(")");
                        }
                    }
                    if (sb.length() == 0 && StrUtil.isNotBlank(isp)) {
                        sb.append(isp);
                    }
                    String result = sb.length() > 0 ? sb.toString() : "未知";
                    IPV6_CACHE.put(ipv6, result);
                    return result;
                }
            }
        } catch (Exception e) {
            log.debug("IPv6 在线归属地查询失败[{}]: {}", ipv6, e.getMessage());
        }
        return "未知";
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
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        String lower = ip.toLowerCase();
        // IPv6 私网、回环、链路本地
        if ("0:0:0:0:0:0:0:1".equals(lower) || "::1".equals(lower)
                || lower.startsWith("fe8") || lower.startsWith("fe9")
                || lower.startsWith("fea") || lower.startsWith("feb")
                || lower.startsWith("fc") || lower.startsWith("fd")
                || lower.startsWith("::ffff:127.")) {
            return true;
        }
        // IPv4 私网与回环
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
                || ip.startsWith("172.31.");
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

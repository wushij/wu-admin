package com.admin.gateway.util;

import cn.dev33.satoken.session.SaSession;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class OnlineSessionHelper {

    public static final String KEY_LOGIN_NAME = "loginName";
    public static final String KEY_DEPT_NAME = "deptName";
    public static final String KEY_IPADDR = "ipaddr";
    public static final String KEY_LOGIN_LOCATION = "loginLocation";
    public static final String KEY_BROWSER = "browser";
    public static final String KEY_OS = "os";
    public static final String KEY_STATUS = "status";
    public static final String KEY_LOGIN_TIME = "loginTime";
    public static final String KEY_LAST_ACCESS_TIME = "lastAccessTime";

    private OnlineSessionHelper() {
    }

    public static void writeLoginSession(SaSession session, String username, String nickname,
                                         String clientIp, String userAgent) {
        long now = System.currentTimeMillis();
        session.set(KEY_LOGIN_NAME, StringUtils.hasText(username) ? username : "");
        session.set(KEY_DEPT_NAME, StringUtils.hasText(nickname) ? nickname : "");
        session.set(KEY_IPADDR, clientIp != null ? clientIp : "");
        session.set(KEY_LOGIN_LOCATION, resolveLocation(clientIp));
        parseUserAgent(userAgent, session);
        session.set(KEY_STATUS, 1);
        session.set(KEY_LOGIN_TIME, now);
        session.set(KEY_LAST_ACCESS_TIME, now);
    }

    public static void touchLastAccess(SaSession session) {
        session.set(KEY_LAST_ACCESS_TIME, System.currentTimeMillis());
    }

    public static String formatTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static String getSessionString(SaSession session, String key) {
        Object value = session.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    public static int getSessionInt(SaSession session, String key, int defaultValue) {
        Object value = session.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static long getSessionLong(SaSession session, String key, long defaultValue) {
        Object value = session.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static void parseUserAgent(String userAgentStr, SaSession session) {
        if (!StringUtils.hasText(userAgentStr)) {
            session.set(KEY_BROWSER, "Unknown");
            session.set(KEY_OS, "Unknown");
            return;
        }
        UserAgent ua = UserAgentUtil.parse(userAgentStr);
        String browser = ua.getBrowser() != null ? ua.getBrowser().getName() : "Unknown";
        String os = ua.getOs() != null ? ua.getOs().getName() : "Unknown";
        session.set(KEY_BROWSER, browser);
        session.set(KEY_OS, os);
    }

    private static String resolveLocation(String ip) {
        if (!StringUtils.hasText(ip)) {
            return "未知";
        }
        if ("127.0.0.1".equals(ip) || ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
            return "内网IP";
        }
        return "未知";
    }
}

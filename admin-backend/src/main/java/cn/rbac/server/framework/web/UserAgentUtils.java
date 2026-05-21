package cn.rbac.server.framework.web;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 User-Agent 与 Client Hints 解析浏览器、操作系统。
 * <p>Windows 11 在 UA 中仍为 {@code Windows NT 10.0}，需结合 {@code Sec-CH-UA-Platform-Version}（通常 ≥ 13）识别。
 */
public final class UserAgentUtils {

    private static final String HDR_PLATFORM = "Sec-CH-UA-Platform";
    private static final String HDR_PLATFORM_VERSION = "Sec-CH-UA-Platform-Version";

    private UserAgentUtils() {
    }

    public static String parseBrowser(String userAgentStr) {
        if (!StringUtils.hasText(userAgentStr)) {
            return "Unknown";
        }
        Matcher edgeMatcher = Pattern.compile("Edg/([\\d.]+)").matcher(userAgentStr);
        if (edgeMatcher.find()) {
            return "MSEdge " + majorVersion(edgeMatcher.group(1));
        }
        Matcher chromeMatcher = Pattern.compile("Chrome/([\\d.]+)").matcher(userAgentStr);
        if (chromeMatcher.find() && !userAgentStr.contains("Edg/")) {
            return "Chrome " + majorVersion(chromeMatcher.group(1));
        }
        Matcher firefoxMatcher = Pattern.compile("Firefox/([\\d.]+)").matcher(userAgentStr);
        if (firefoxMatcher.find()) {
            return "Firefox " + majorVersion(firefoxMatcher.group(1));
        }
        Matcher safariMatcher = Pattern.compile("Version/([\\d.]+).*Safari").matcher(userAgentStr);
        if (safariMatcher.find() && !userAgentStr.contains("Chrome/") && !userAgentStr.contains("Edg/")) {
            return "Safari " + majorVersion(safariMatcher.group(1));
        }
        UserAgent userAgent = UserAgentUtil.parse(userAgentStr);
        String fallbackName = userAgent.getBrowser().getName();
        return (fallbackName == null || fallbackName.isEmpty()) ? "Unknown" : fallbackName;
    }

    /**
     * 优先 Client Hints，再解析 User-Agent。
     */
    public static String parseOs(HttpServletRequest request) {
        if (request == null) {
            return "Unknown";
        }
        String fromHints = resolveOsFromClientHints(request);
        if (fromHints != null) {
            return fromHints;
        }
        return parseOsFromUserAgent(request.getHeader("User-Agent"));
    }

    public static String parseOsFromUserAgent(String userAgentStr) {
        if (!StringUtils.hasText(userAgentStr)) {
            return "Unknown";
        }
        if (userAgentStr.contains("Windows NT 10.0")) {
            if (userAgentStr.contains("Windows 11") || userAgentStr.contains("Win11")) {
                return "Windows 11";
            }
            // 无 Client Hints 时无法从 UA 区分 Win10/Win11，避免误标为 Win10
            return "Windows";
        }
        if (userAgentStr.contains("Windows NT 6.3")) {
            return "Windows 8.1";
        }
        if (userAgentStr.contains("Windows NT 6.2")) {
            return "Windows 8";
        }
        if (userAgentStr.contains("Windows NT 6.1")) {
            return "Windows 7";
        }
        if (userAgentStr.contains("Windows NT 6.0")) {
            return "Windows Vista";
        }
        if (userAgentStr.contains("Windows NT 5.1")) {
            return "Windows XP";
        }

        Matcher androidMatcher = Pattern.compile("Android ([\\d.]+)").matcher(userAgentStr);
        if (androidMatcher.find()) {
            return "Android " + androidMatcher.group(1);
        }

        Matcher iosMatcher = Pattern.compile("(?:iPhone|CPU (?:iPhone )?OS) ([\\d_]+)").matcher(userAgentStr);
        if (iosMatcher.find()) {
            return "iOS " + iosMatcher.group(1).replace("_", ".");
        }

        Matcher macMatcher = Pattern.compile("Mac OS X ([\\d_]+)").matcher(userAgentStr);
        if (macMatcher.find()) {
            return "macOS " + macMatcher.group(1).replace("_", ".");
        }

        if (userAgentStr.contains("Ubuntu")) {
            return "Ubuntu";
        }
        if (userAgentStr.contains("CentOS")) {
            return "CentOS";
        }
        if (userAgentStr.contains("Fedora")) {
            return "Fedora";
        }
        if (userAgentStr.contains("Debian")) {
            return "Debian";
        }
        if (userAgentStr.contains("Linux")) {
            return "Linux";
        }

        UserAgent userAgent = UserAgentUtil.parse(userAgentStr);
        String fallbackOs = userAgent.getOs().getName();
        return (fallbackOs == null || fallbackOs.isEmpty()) ? "Unknown" : fallbackOs;
    }

    /**
     * @see <a href="https://learn.microsoft.com/en-us/microsoft-edge/web-platform/user-agent-guidance">Microsoft UA guidance</a>
     */
    static String resolveOsFromClientHints(HttpServletRequest request) {
        String platform = trimHint(request.getHeader(HDR_PLATFORM));
        String platformVersion = trimHint(request.getHeader(HDR_PLATFORM_VERSION));
        if (!"Windows".equalsIgnoreCase(platform) || !StringUtils.hasText(platformVersion)) {
            return null;
        }
        try {
            int major = Integer.parseInt(platformVersion.split("\\.")[0]);
            if (major >= 13) {
                return "Windows 11";
            }
            if (major >= 10) {
                return "Windows 10";
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private static String trimHint(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\"", "").trim();
    }

    private static String majorVersion(String version) {
        if (version == null || version.isEmpty()) {
            return "";
        }
        String[] parts = version.split("\\.");
        return parts.length > 0 ? parts[0] : version;
    }
}

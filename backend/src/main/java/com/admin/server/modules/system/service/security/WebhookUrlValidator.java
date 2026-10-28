package com.admin.server.modules.system.service.security;

import cn.hutool.core.util.StrUtil;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * 告警 webhook 地址的 SSRF 防护校验。
 *
 * <p>规则：</p>
 * <ol>
 *   <li>仅允许 http/https，且不允许携带 userinfo（{@code user:pass@host} 常用于绕过域名判断）；</li>
 *   <li>主机名解析出的<b>所有</b>地址都必须是公网地址，拒绝回环、通配、链路本地（含云元数据
 *       169.254.169.254）、私网、CGNAT、组播、保留网段以及 IPv6 唯一本地地址；</li>
 *   <li>配置保存时与实际推送前各校验一次，推送侧还会显式关闭重定向，避免 302 跳转到内网。</li>
 * </ol>
 *
 * <p>局限：推送前校验与实际建连之间仍存在极短的 DNS 重绑定窗口，
 * 如需彻底杜绝，应在出口网关/防火墙层面再限制该服务对内网的访问。</p>
 */
public final class WebhookUrlValidator {

    private WebhookUrlValidator() {
    }

    /**
     * 校验 webhook 地址，非法时抛出 {@link IllegalArgumentException}，消息可直接展示给管理员。
     *
     * @param url webhook 地址，不能为空
     * @return 通过校验的 URI
     */
    public static URI validate(String url) {
        if (StrUtil.isBlank(url)) {
            throw new IllegalArgumentException("告警 webhook 不能为空");
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("告警 webhook 不是合法的 URL");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("告警 webhook 必须以 http:// 或 https:// 开头");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("告警 webhook 不允许携带用户名密码");
        }
        String host = uri.getHost();
        if (StrUtil.isBlank(host)) {
            throw new IllegalArgumentException("告警 webhook 缺少主机名");
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("告警 webhook 主机名无法解析: " + host);
        }
        for (InetAddress address : addresses) {
            if (isForbiddenAddress(address)) {
                throw new IllegalArgumentException("告警 webhook 不允许指向回环、内网或链路本地地址");
            }
        }
        return uri;
    }

    /** 是否为禁止访问的地址（非公网） */
    static boolean isForbiddenAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return true;
        }
        byte[] b = address.getAddress();
        if (address instanceof Inet4Address) {
            int first = b[0] & 0xff;
            int second = b[1] & 0xff;
            int third = b[2] & 0xff;
            // 0.0.0.0/8 本网络
            if (first == 0) {
                return true;
            }
            // 100.64.0.0/10 运营商级 NAT
            if (first == 100 && second >= 64 && second <= 127) {
                return true;
            }
            // 192.0.0.0/24 IETF 协议分配
            if (first == 192 && second == 0 && third == 0) {
                return true;
            }
            // 198.18.0.0/15 基准测试网段
            if (first == 198 && (second == 18 || second == 19)) {
                return true;
            }
            // 240.0.0.0/4 保留地址（含 255.255.255.255 广播）
            return first >= 240;
        }
        if (address instanceof Inet6Address) {
            // fc00::/7 唯一本地地址，JDK 的 isSiteLocalAddress 不覆盖该网段
            return (b[0] & 0xfe) == 0xfc;
        }
        return false;
    }

    /** 便于日志脱敏：只展示协议、主机与端口，不输出路径和查询参数（常含机器人 token） */
    public static String mask(String url) {
        if (StrUtil.isBlank(url)) {
            return "";
        }
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? "?" : uri.getHost();
            String port = uri.getPort() > 0 ? ":" + uri.getPort() : "";
            return scheme + "://" + host + port + "/***";
        } catch (IllegalArgumentException e) {
            return "***";
        }
    }
}

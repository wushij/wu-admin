package com.admin.server.modules.system.service.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("告警 webhook SSRF 校验单元测试")
class WebhookUrlValidatorTest {

    @Test
    @DisplayName("拒绝回环、私网、链路本地（云元数据）、CGNAT、IPv6 内网等地址")
    void rejectInternalAddresses() {
        String[] forbidden = {
                "http://127.0.0.1:8080/hook",
                "http://localhost/hook",
                "http://0.0.0.0/hook",
                "http://10.1.2.3/hook",
                "http://172.16.0.1/hook",
                "http://192.168.1.1/hook",
                "http://169.254.169.254/latest/meta-data/",
                "http://100.64.0.1/hook",
                "http://[::1]/hook",
                "http://[fd00::1]/hook",
                "http://[::ffff:127.0.0.1]/hook",
                "http://255.255.255.255/hook"
        };
        for (String url : forbidden) {
            assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.validate(url), url);
        }
    }

    @Test
    @DisplayName("拒绝非 http/https 协议、userinfo 绕过与空地址")
    void rejectMalformed() {
        assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.validate(null));
        assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.validate(" "));
        assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.validate("ftp://8.8.8.8/x"));
        assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.validate("file:///etc/passwd"));
        assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate("http://8.8.8.8@127.0.0.1/hook"));
    }

    @Test
    @DisplayName("公网 IP 字面量通过校验")
    void allowPublicAddress() {
        assertDoesNotThrow(() -> WebhookUrlValidator.validate("https://8.8.8.8/hook"));
        assertDoesNotThrow(() -> WebhookUrlValidator.validate("http://1.1.1.1:8443/hook?token=abc"));
    }

    @Test
    @DisplayName("日志脱敏只保留协议、主机与端口，不泄露机器人 token")
    void maskHidesPathAndQuery() {
        String masked = WebhookUrlValidator.mask("https://oapi.example.com:8443/robot/send?access_token=SECRET");
        assertTrue(masked.startsWith("https://oapi.example.com:8443"));
        assertTrue(!masked.contains("SECRET"));
    }
}
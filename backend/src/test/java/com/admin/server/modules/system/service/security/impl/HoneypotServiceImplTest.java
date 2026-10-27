package com.admin.server.modules.system.service.security.impl;

import cn.hutool.json.JSONUtil;
import com.admin.server.framework.security.core.HoneypotPaths;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.admin.server.modules.system.service.security.AntiBotRiskService;
import com.admin.server.modules.system.service.security.HoneypotResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("蜜罐诱捕服务单元测试")
class HoneypotServiceImplTest {

    @Mock
    private AntiBotRiskService antiBotRiskService;

    @Mock
    private SystemConfigHelper systemConfigHelper;

    @InjectMocks
    private HoneypotServiceImpl honeypotService;

    @Test
    @DisplayName("isEnabled：读取系统配置开关")
    void testIsEnabled() {
        when(systemConfigHelper.isHoneypotEnabled()).thenReturn(true);
        assertTrue(honeypotService.isEnabled());

        when(systemConfigHelper.isHoneypotEnabled()).thenReturn(false);
        assertFalse(honeypotService.isEnabled());
    }

    @Test
    @DisplayName("hit：命中后按 IP 加分并封禁，响应体是诱饵数据")
    void testHitRecordsRiskAndReturnsDecoy() {
        when(systemConfigHelper.getHoneypotRiskDelta()).thenReturn(80);
        when(systemConfigHelper.getHoneypotBlockSeconds()).thenReturn(86400L);
        when(systemConfigHelper.getHoneypotTarpitMs()).thenReturn(0);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/system/internal/db-backup");
        request.setMethod("GET");
        request.setRemoteAddr("1.2.3.4");
        request.addHeader("User-Agent", "curl/8.0");

        HoneypotResult result = honeypotService.hit(request, "db-backup");

        // 记分与封禁都必须落在真实 IP 上，换成设备标识会被伪造绕过
        verify(antiBotRiskService).addRisk(eq("1.2.3.4"), eq("1.2.3.4"), eq(80),
                eq("HONEYPOT"), eq("FORBIDDEN"), contains("/system/internal/db-backup"),
                eq("GET"), anyString(), contains("trap=db-backup"));
        verify(antiBotRiskService).blockPrincipal(eq("1.2.3.4"), eq(86400L), anyString());

        assertEquals(200, result.getHttpStatus());
        String body = JSONUtil.toJsonStr(result.getBody());
        assertTrue(body.contains("export_id"), "诱饵响应应带 export_id，伪装成真实导出接口");
        assertTrue(body.contains("sys_user"), "db-backup 诱饵应伪装成数据库导出");
        // canary 用于追溯泄露来源，格式为 hk + 10 位十六进制
        assertTrue(body.matches("(?s).*\\bhk[0-9a-f]{10}\\b.*"), "诱饵必须嵌入 canary 标识");
    }

    @Test
    @DisplayName("resolveTrapId：从带 context-path 的路径解析诱饵标识")
    void testResolveTrapId() {
        assertEquals("db-backup", HoneypotPaths.resolveTrapId("/api/system/internal/db-backup"));
        assertEquals("config-export", HoneypotPaths.resolveTrapId("/system/internal/config-export"));
        assertEquals("console", HoneypotPaths.resolveTrapId("/api/system/internal/console/debug/exec"));
        assertEquals("unknown", HoneypotPaths.resolveTrapId(null));
    }
}

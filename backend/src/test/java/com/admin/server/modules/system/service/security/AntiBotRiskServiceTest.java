package com.admin.server.modules.system.service.security;

import com.admin.server.framework.security.core.RiskPrincipal;
import com.admin.server.modules.system.dal.dataobject.security.SecurityEventDO;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.admin.server.modules.system.service.security.impl.AntiBotRiskServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("行为风控服务单元测试")
class AntiBotRiskServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RScript script;

    @Mock
    private RBucket<Object> bucket;

    @Mock
    private RBucket<Object> blockedBucket;

    @Mock
    private RBucket<Object> clearBucket;

    @Mock
    private SecurityEventService securityEventService;

    @Mock
    private SystemConfigHelper systemConfigHelper;

    @Mock
    private RiskAlertNotifier riskAlertNotifier;

    @InjectMocks
    private AntiBotRiskServiceImpl antiBotRiskService;

    @Test
    @DisplayName("matchBlockedUserAgent：准确识别自动化爬虫与扫描器 UA")
    void testMatchBlockedUserAgent() {
        assertNotNull(antiBotRiskService.matchBlockedUserAgent("python-requests/2.31.0"));
        assertNotNull(antiBotRiskService.matchBlockedUserAgent("curl/8.1.2"));
        assertNotNull(antiBotRiskService.matchBlockedUserAgent("Scrapy/2.11.0 (+https://scrapy.org)"));
        assertNotNull(antiBotRiskService.matchBlockedUserAgent("Mozilla/5.0 (compatible; Go-http-client/1.1)"));
        assertNotNull(antiBotRiskService.matchBlockedUserAgent("HeadlessChrome/118.0.5993.88"));
        assertNotNull(antiBotRiskService.matchBlockedUserAgent("sqlmap/1.7.2"));
        assertNotNull(antiBotRiskService.matchBlockedUserAgent("Nikto/2.5.0"));

        // 正常浏览器 User-Agent 不应误报
        assertNull(antiBotRiskService.matchBlockedUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"));
        assertNull(antiBotRiskService.matchBlockedUserAgent(null));
        assertNull(antiBotRiskService.matchBlockedUserAgent(""));
    }

    @Test
    @DisplayName("RiskLevel：按阈值映射五个处置等级")
    void testRiskLevelMapping() {
        assertEquals(RiskLevel.NORMAL, RiskLevel.of(0, 30, 50, 70, 90));
        assertEquals(RiskLevel.NOTICE, RiskLevel.of(30, 30, 50, 70, 90));
        assertEquals(RiskLevel.WARNING, RiskLevel.of(50, 30, 50, 70, 90));
        assertEquals(RiskLevel.CHALLENGE, RiskLevel.of(70, 30, 50, 70, 90));
        assertEquals(RiskLevel.BLOCK, RiskLevel.of(90, 30, 50, 70, 90));

        // 六档重载：临时封禁与最高档封禁必须能区分
        assertEquals(RiskLevel.BLOCK, RiskLevel.of(90, 30, 50, 70, 90, 120));
        assertEquals(RiskLevel.BAN, RiskLevel.of(120, 30, 50, 70, 90, 120));
        assertEquals(5, RiskLevel.BAN.getCode());
    }

    @Test
    @DisplayName("RiskPrincipal：裸 IP 与带前缀标识都能归一化为同一主体")
    void testPrincipalNormalize() {
        assertEquals("ip:1.2.3.4", RiskPrincipal.normalize("1.2.3.4", "1.2.3.4").key());
        assertEquals("ip:1.2.3.4", RiskPrincipal.normalize("ip:1.2.3.4", null).key());
        assertEquals("user:12", RiskPrincipal.normalize("user:12", "1.2.3.4").key());
        assertEquals("user:12", RiskPrincipal.ofUser(12L, "1.2.3.4").key());
        assertEquals("user", RiskPrincipal.ofUser(12L, null).type());
        assertEquals("ip:unknown", RiskPrincipal.normalize("", "").key());
    }

    /** 统一桩：风控开启 + 默认阈值 + Lua 汇总脚本返回指定总分 */
    private void stubRisk(long totalScore) {
        stubRiskWith(List.of(totalScore, 1L, System.currentTimeMillis()));
    }

    /** 同上，但可自定义 Lua 返回值（用于验证字符串返回值） */
    private void stubRiskWith(List<Object> scriptResult) {
        when(systemConfigHelper.isRiskEnabled()).thenReturn(true);
        when(systemConfigHelper.getRiskWindowMinutes()).thenReturn(60);
        when(systemConfigHelper.getRiskNoticeScore()).thenReturn(30);
        when(systemConfigHelper.getRiskWarnScore()).thenReturn(50);
        when(systemConfigHelper.getRiskChallengeScore()).thenReturn(70);
        when(systemConfigHelper.getRiskBlockScore()).thenReturn(90);
        when(systemConfigHelper.getRiskBanScore()).thenReturn(120);
        when(systemConfigHelper.getRiskBlockSeconds()).thenReturn(3600L);
        doReturn(script).when(redissonClient).getScript(any(org.redisson.client.codec.Codec.class));
        doReturn(scriptResult).when(script)
                .eval(any(RScript.Mode.class), anyString(), any(RScript.ReturnType.class), anyList(),
                        any(), any(), any(), any(), any());
        doReturn(bucket).when(redissonClient).getBucket(anyString());
    }

    @Test
    @DisplayName("addRisk：累加窗口分，达到临时封禁阈值时写入封禁标记，且分桶脚本只调用一次")
    void testAddRiskAutoBlock() {
        stubRisk(95);

        int total = antiBotRiskService.addRisk("1.2.3.4", "1.2.3.4", 40,
                "UA_BLOCK", "BLOCKED", "/api/test", "GET", "python-requests/2.28", "命中UA");

        assertEquals(95, total);
        verify(script, times(1)).eval(any(RScript.Mode.class), anyString(), any(RScript.ReturnType.class),
                anyList(), any(), any(), any(), any(), any());
        verify(bucket).set(any(), any(java.time.Duration.class));
        verify(securityEventService).recordAsync(any(SecurityEventDO.class));
    }

    @Test
    @DisplayName("addRisk：风控总开关关闭时不记分，也不触达 Redis 写入")
    void testAddRiskDisabled() {
        when(systemConfigHelper.isRiskEnabled()).thenReturn(false);
        doReturn(script).when(redissonClient).getScript(any(org.redisson.client.codec.Codec.class));
        doReturn(List.of(0L, 0L, 0L)).when(script)
                .eval(any(RScript.Mode.class), anyString(), any(RScript.ReturnType.class), anyList(),
                        any(), any(), any(), any(), any());

        antiBotRiskService.addRisk("1.2.3.4", "1.2.3.4", 40,
                "UA_BLOCK", "BLOCKED", "/api/test", "GET", "ua", "reason");

        verify(securityEventService, never()).recordAsync(any(SecurityEventDO.class));
        verify(riskAlertNotifier, never()).notify(any(), any(), any(), anyInt(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("告警去重：同一主体同一事件同一等级在去重期内只推送一次")
    void testAlertDeduplicated() {
        stubRisk(95);
        // 第一次拿到去重许可，之后都被拒绝
        when(bucket.setIfAbsent(any(), any(java.time.Duration.class))).thenReturn(true, false, false);

        for (int i = 0; i < 3; i++) {
            antiBotRiskService.addRisk("1.2.3.4", "1.2.3.4", 15,
                    "SIGN_FAIL", "BLOCKED", "/api/test", "POST", "ua", "签名不匹配");
        }

        verify(riskAlertNotifier, times(1)).notify(eq("SIGN_FAIL"), eq("ip:1.2.3.4"), eq("临时封禁"),
                eq(95), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("blockPrincipal：已有封禁剩余时长更长时不被较短的写入覆盖")
    void testBlockNeverShortens() {
        doReturn(bucket).when(redissonClient).getBucket(anyString());
        // 剩余 7 天，新请求只有 1 天
        when(bucket.remainTimeToLive()).thenReturn(7 * 86400_000L);

        antiBotRiskService.blockPrincipal("1.2.3.4", 86400L, "蜜罐");

        verify(bucket, never()).set(any(), any(java.time.Duration.class));
    }

    @Test
    @DisplayName("blockPrincipal：永久封禁不被覆盖；剩余更短或不存在时正常写入")
    void testBlockExtendsWhenShorter() {
        doReturn(bucket).when(redissonClient).getBucket(anyString());
        when(bucket.remainTimeToLive()).thenReturn(-1L, 600_000L, -2L);

        antiBotRiskService.blockPrincipal("1.2.3.4", 86400L, "a");
        antiBotRiskService.blockPrincipal("1.2.3.4", 86400L, "b");
        antiBotRiskService.blockPrincipal("1.2.3.4", 86400L, "c");

        // 第一次永久封禁不动，后两次（剩余 10 分钟、不存在）写入
        verify(bucket, times(2)).set(any(), any(java.time.Duration.class));
    }

    @Test
    @DisplayName("Lua 返回值为字符串时同样能正确取分，不会静默变成 0")
    void testScriptStringResultStillCounted() {
        stubRiskWith(List.of("95", "1", "123"));

        int total = antiBotRiskService.addRisk("1.2.3.4", "1.2.3.4", 40,
                "UA_BLOCK", "BLOCKED", "/api/test", "GET", "ua", "reason");

        assertEquals(95, total);
    }
    @Test
    @DisplayName("总开关关闭后，历史风险分不再触发挑战")
    void testChallengeNotRequiredWhenRiskDisabled() {
        when(systemConfigHelper.isRiskEnabled()).thenReturn(false);

        assertFalse(antiBotRiskService.isChallengeRequired(RiskPrincipal.ofIp("1.2.3.4")));
    }
    @Test
    @DisplayName("isBlocked：命中封禁标记返回 true，空 IP 返回 false")
    void testIsBlocked() {
        when(redissonClient.getBucket("security:risk:block:ip:1.2.3.4")).thenReturn(blockedBucket);
        when(redissonClient.getBucket("security:risk:block:ip:5.6.7.8")).thenReturn(clearBucket);
        when(redissonClient.getBucket("security:risk:block:5.6.7.8")).thenReturn(clearBucket);
        when(blockedBucket.isExists()).thenReturn(true);
        when(clearBucket.isExists()).thenReturn(false);

        assertTrue(antiBotRiskService.isBlocked("1.2.3.4"));
        assertFalse(antiBotRiskService.isBlocked("5.6.7.8"));
    }
}

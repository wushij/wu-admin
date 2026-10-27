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
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;

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
    private RScoredSortedSet<String> scoredSet;

    @Mock
    private RBucket<Object> bucket;

    @Mock
    private RBucket<Object> blockedBucket;

    @Mock
    private RBucket<Object> clearBucket;

    @Mock
    private ScoredEntry<String> entry;

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

    @Test
    @DisplayName("addRisk：累加窗口内分值并在达到阈值时写入封禁")
    void testAddRiskAutoBlock() {
        when(systemConfigHelper.isRiskEnabled()).thenReturn(true);
        when(systemConfigHelper.getRiskWindowMinutes()).thenReturn(60);
        when(systemConfigHelper.getRiskNoticeScore()).thenReturn(30);
        when(systemConfigHelper.getRiskWarnScore()).thenReturn(50);
        when(systemConfigHelper.getRiskChallengeScore()).thenReturn(70);
        when(systemConfigHelper.getRiskBlockScore()).thenReturn(90);
        when(systemConfigHelper.getRiskBanScore()).thenReturn(120);
        when(systemConfigHelper.getRiskBlockSeconds()).thenReturn(3600L);

        doReturn(scoredSet).when(redissonClient).getScoredSortedSet(anyString());
        doReturn(bucket).when(redissonClient).getBucket(anyString());

        when(entry.getValue()).thenReturn("95|test");
        doReturn(List.of(entry)).when(scoredSet)
                .entryRange(anyDouble(), anyBoolean(), anyDouble(), anyBoolean());

        int total = antiBotRiskService.addRisk("1.2.3.4", "1.2.3.4", 40,
                "UA_BLOCK", "BLOCKED", "/api/test", "GET", "python-requests/2.28", "命中爬虫UA");

        assertEquals(95, total);
        verify(scoredSet).add(anyDouble(), contains("40|"));
        verify(systemConfigHelper).getRiskBlockScore();
        verify(securityEventService).recordAsync(any(SecurityEventDO.class));
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

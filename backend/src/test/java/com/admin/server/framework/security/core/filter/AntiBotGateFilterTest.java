package com.admin.server.framework.security.core.filter;

import com.admin.server.framework.web.core.ApiErrorResponseWriter;
import com.admin.server.framework.config.DynamicConfigProvider;
import com.admin.server.framework.security.core.RiskPrincipal;
import com.admin.server.framework.security.core.RiskScoringApi;
import com.admin.server.framework.security.core.SliderChallengeVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AntiBotGateFilter 单元测试")
class AntiBotGateFilterTest {

    private AntiBotGateFilter filter;
    private RiskScoringApi antiBotRiskService;
    private DynamicConfigProvider dynamicConfigProvider;
    private RedissonClient redissonClient;

    @BeforeEach
    void setUp() {
        filter = new AntiBotGateFilter();
        antiBotRiskService = mock(RiskScoringApi.class);
        dynamicConfigProvider = mock(DynamicConfigProvider.class);
        redissonClient = mock(RedissonClient.class);
        ReflectionTestUtils.setField(filter, "antiBotRiskService", antiBotRiskService);
        ReflectionTestUtils.setField(filter, "dynamicConfigProvider", dynamicConfigProvider);
        ReflectionTestUtils.setField(filter, "redissonClient", redissonClient);
        ReflectionTestUtils.setField(filter, "sliderCaptchaService", mock(SliderChallengeVerifier.class));
        ReflectionTestUtils.setField(filter, "errorResponseWriter",
                new ApiErrorResponseWriter(new ObjectMapper()));
        // 默认关闭行为识别与挑战，避免无关用例触发额外 Redis 交互
        lenient().when(dynamicConfigProvider.isRiskEnabled()).thenReturn(false);
        lenient().when(dynamicConfigProvider.isRiskBehaviorEnabled()).thenReturn(false);
        lenient().when(dynamicConfigProvider.isRiskChallengeEnabled()).thenReturn(false);
    }

    @Test
    @DisplayName("被封禁 IP 的 multipart 请求在门禁层直接 403")
    void blockedIpIsRejectedBeforeMultipart() throws Exception {
        when(antiBotRiskService.isBlocked("1.2.3.4")).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRemoteAddr("1.2.3.4");
        request.setContentType("multipart/form-data; boundary=----test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> invoked.set(true);

        filter.doFilterInternal(request, response, chain);

        assertEquals(403, response.getStatus());
        assertFalse(invoked.get());
    }

    @Test
    @DisplayName("命中爬虫 UA 时记录风险分并 403")
    void blockedUaIsRejected() throws Exception {
        when(antiBotRiskService.isBlocked(anyString())).thenReturn(false);
        when(antiBotRiskService.matchBlockedUserAgent("python-requests/2.31.0")).thenReturn("python-requests");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "python-requests/2.31.0");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> invoked.set(true);

        filter.doFilterInternal(request, response, chain);

        assertEquals(403, response.getStatus());
        assertFalse(invoked.get());
        verify(antiBotRiskService).addRisk(any(RiskPrincipal.class), eq(40),
                eq("UA_BLOCK"), eq("BLOCKED"), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("风控存储故障时放行请求，避免静态资源与登录页不可用")
    void redisFailureFailsOpen() throws Exception {
        when(antiBotRiskService.isBlocked(anyString()))
                .thenThrow(new RuntimeException("redis down"));
        when(antiBotRiskService.matchBlockedUserAgent(any())).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "Mozilla/5.0");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> invoked.set(true);

        filter.doFilterInternal(request, response, chain);

        assertTrue(invoked.get());
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("正常浏览器请求继续执行过滤器链")
    void normalRequestPasses() throws Exception {
        when(antiBotRiskService.isBlocked(anyString())).thenReturn(false);
        when(antiBotRiskService.matchBlockedUserAgent(any())).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "Mozilla/5.0");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> invoked.set(true);

        filter.doFilterInternal(request, response, chain);

        assertTrue(invoked.get());
        assertEquals(200, response.getStatus());
    }
}

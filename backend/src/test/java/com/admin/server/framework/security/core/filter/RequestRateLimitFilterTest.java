package com.admin.server.framework.security.core.filter;

import com.admin.server.framework.config.DynamicConfigProvider;
import com.admin.server.framework.web.core.ApiErrorResponseWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("RequestRateLimitFilter 单元测试")
class RequestRateLimitFilterTest {

    private RequestRateLimitFilter filter;
    private StringRedisTemplate stringRedisTemplate;
    private DynamicConfigProvider systemConfigHelper;

    @BeforeEach
    void setUp() {
        filter = new RequestRateLimitFilter();
        stringRedisTemplate = mock(StringRedisTemplate.class);
        systemConfigHelper = mock(DynamicConfigProvider.class);

        ReflectionTestUtils.setField(filter, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(filter, "dynamicConfigProvider", systemConfigHelper);
        ReflectionTestUtils.setField(filter, "errorResponseWriter",
                new ApiErrorResponseWriter(new ObjectMapper()));
    }

    /** 模拟 Lua 脚本返回的窗口内计数 */
    private void stubWindowCount(Long count) {
        when(stringRedisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any(Object[].class)))
                .thenReturn(count);
    }

    @Test
    @DisplayName("同一 IP 超过业务 API 配额时返回 429")
    void rejectsOverLimitApiRequest() throws Exception {
        when(systemConfigHelper.getApiPerIpMinute()).thenReturn(1);
        stubWindowCount(2L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/system/user/list");
        request.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> invoked.set(true);

        filter.doFilterInternal(request, response, chain);

        assertEquals(429, response.getStatus());
        assertFalse(invoked.get());
    }

    @Test
    @DisplayName("未超过配额时继续执行过滤器链")
    void allowsUnderLimitRequest() throws Exception {
        when(systemConfigHelper.getApiPerIpMinute()).thenReturn(10);
        stubWindowCount(3L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/system/user/list");
        request.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> invoked.set(true);

        filter.doFilterInternal(request, response, chain);

        assertTrue(invoked.get());
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("配额为 0 时不限流，也不访问 Redis")
    void skipsWhenLimitDisabled() throws Exception {
        when(systemConfigHelper.getApiPerIpMinute()).thenReturn(0);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/system/user/list");
        request.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> invoked.set(true);

        filter.doFilterInternal(request, response, chain);

        assertTrue(invoked.get());
    }

    @Test
    @DisplayName("Redis 异常时 fail-open，保证站点可用")
    void failsOpenWhenRedisUnavailable() throws Exception {
        when(systemConfigHelper.getApiPerIpMinute()).thenReturn(1);
        when(stringRedisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any(Object[].class)))
                .thenThrow(new IllegalStateException("redis down"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/system/user/list");
        request.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> invoked.set(true);

        filter.doFilterInternal(request, response, chain);

        assertTrue(invoked.get());
    }

    @Test
    @DisplayName("认证接口与文件访问分别使用独立配额分组")
    void usesSeparateGroups() throws Exception {
        when(systemConfigHelper.getAuthPerIpMinute()).thenReturn(2);
        stubWindowCount(1L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> invoked.set(true);

        filter.doFilterInternal(request, response, chain);

        assertTrue(invoked.get());
        // 认证分组命中后不应继续读取通用 API 配额
        org.mockito.Mockito.verify(systemConfigHelper).getAuthPerIpMinute();
        org.mockito.Mockito.verify(systemConfigHelper, org.mockito.Mockito.never()).getApiPerIpMinute();
    }

    @Test
    @DisplayName("文件直出和 OPTIONS 预检不参与应用层限流")
    void skipsFileDirectAndOptions() {
        MockHttpServletRequest fileRequest = new MockHttpServletRequest();
        fileRequest.setMethod("GET");
        fileRequest.setRequestURI("/api/files/2026/10/a.png");
        assertTrue(filter.shouldNotFilter(fileRequest));

        MockHttpServletRequest optionsRequest = new MockHttpServletRequest();
        optionsRequest.setMethod("OPTIONS");
        optionsRequest.setRequestURI("/api/system/user/list");
        assertTrue(filter.shouldNotFilter(optionsRequest));
    }
}

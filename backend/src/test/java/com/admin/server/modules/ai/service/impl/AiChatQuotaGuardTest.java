package com.admin.server.modules.ai.service.impl;

import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.admin.server.modules.system.service.permission.PermissionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AiChatQuotaGuard 单元测试")
class AiChatQuotaGuardTest {

    private static final long USER_ID = 100L;

    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final SystemConfigHelper systemConfigHelper = mock(SystemConfigHelper.class);
    private final PermissionService permissionService = mock(PermissionService.class);

    private AiChatQuotaGuard guard;

    private AiChatQuotaGuard newGuard() {
        AiChatQuotaGuard g = new AiChatQuotaGuard();
        ReflectionTestUtils.setField(g, "redissonClient", redissonClient);
        ReflectionTestUtils.setField(g, "systemConfigHelper", systemConfigHelper);
        ReflectionTestUtils.setField(g, "permissionService", permissionService);
        return g;
    }

    /** 打桩分钟计数器：incrementAndGet 返回指定值 */
    private void stubMinuteCounter(long value) {
        RAtomicLong counter = mock(RAtomicLong.class);
        when(counter.incrementAndGet()).thenReturn(value);
        when(redissonClient.getAtomicLong(startsWith("ai:rl:chat:"))).thenReturn(counter);
    }

    /** 打桩 token 配额计数器：get 返回已用量 */
    private void stubTokenCounter(long used) {
        RAtomicLong counter = mock(RAtomicLong.class);
        when(counter.get()).thenReturn(used);
        when(redissonClient.getAtomicLong(startsWith("ai:quota:tokens:"))).thenReturn(counter);
    }

    @Test
    @DisplayName("两项配置均为 0：放行，不触碰 Redis 计数")
    void check_allDisabled_pass() {
        guard = newGuard();
        when(systemConfigHelper.getAiChatPerUserMinute()).thenReturn(0);
        when(systemConfigHelper.getAiTokensPerUserDaily()).thenReturn(0L);
        when(systemConfigHelper.getAiRoleTokenQuotas()).thenReturn(Map.of());

        assertNull(guard.check(USER_ID));
        verify(redissonClient, never()).getAtomicLong(anyString());
    }

    @Test
    @DisplayName("分钟内第 N 次（N ≤ 上限）：放行且计数自增，首次设 TTL")
    void check_underMinuteLimit_pass() {
        guard = newGuard();
        when(systemConfigHelper.getAiChatPerUserMinute()).thenReturn(8);
        when(systemConfigHelper.getAiTokensPerUserDaily()).thenReturn(0L);
        when(systemConfigHelper.getAiRoleTokenQuotas()).thenReturn(Map.of());
        RAtomicLong counter = mock(RAtomicLong.class);
        when(counter.incrementAndGet()).thenReturn(1L);
        when(redissonClient.getAtomicLong(startsWith("ai:rl:chat:"))).thenReturn(counter);

        assertNull(guard.check(USER_ID));
        verify(counter).incrementAndGet();
        verify(counter).expire(java.time.Duration.ofMinutes(2));
    }

    @Test
    @DisplayName("分钟内超限：返回频率文案")
    void check_overMinuteLimit_rejected() {
        guard = newGuard();
        when(systemConfigHelper.getAiChatPerUserMinute()).thenReturn(8);
        stubMinuteCounter(9L);

        String result = guard.check(USER_ID);
        assertEquals("提问太频繁了，请稍候再试 🍵", result);
    }

    @Test
    @DisplayName("日 token 已达配额：返回配额文案")
    void check_tokenQuotaExceeded_rejected() {
        guard = newGuard();
        when(systemConfigHelper.getAiChatPerUserMinute()).thenReturn(0);
        when(systemConfigHelper.getAiTokensPerUserDaily()).thenReturn(100000L);
        when(systemConfigHelper.getAiRoleTokenQuotas()).thenReturn(Map.of());
        stubTokenCounter(100000L);

        assertEquals("今日 AI 用量已达上限，明天再来吧", guard.check(USER_ID));
    }

    @Test
    @DisplayName("多角色命中多条规则：按最大配额生效")
    void resolveQuota_multiRole_takesMax() {
        guard = newGuard();
        when(systemConfigHelper.getAiChatPerUserMinute()).thenReturn(0);
        when(systemConfigHelper.getAiTokensPerUserDaily()).thenReturn(100000L);
        when(systemConfigHelper.getAiRoleTokenQuotas()).thenReturn(Map.of(1L, 300000L, 3L, 500000L));
        when(permissionService.getUserRoleIdListByUserId(USER_ID)).thenReturn(Set.of(1L, 3L));
        // 已用 400000 < max(300000,500000)=500000 → 放行
        stubTokenCounter(400000L);

        assertNull(guard.check(USER_ID));
    }

    @Test
    @DisplayName("角色未命中任何规则（含角色已删除）：落兜底 tokensPerUserDaily")
    void resolveQuota_noRoleMatch_fallback() {
        guard = newGuard();
        when(systemConfigHelper.getAiChatPerUserMinute()).thenReturn(0);
        when(systemConfigHelper.getAiTokensPerUserDaily()).thenReturn(100000L);
        when(systemConfigHelper.getAiRoleTokenQuotas()).thenReturn(Map.of(99L, 500000L));
        when(permissionService.getUserRoleIdListByUserId(USER_ID)).thenReturn(Set.of(1L, 2L));
        // 用户角色 1/2 都不在规则里 → 用兜底 100000，已用 100000 达上限 → 拒绝
        stubTokenCounter(100000L);

        assertEquals("今日 AI 用量已达上限，明天再来吧", guard.check(USER_ID));
    }

    @Test
    @DisplayName("角色规则配 0：该用户不限量（即使兜底值非 0）")
    void resolveQuota_roleZero_unlimited() {
        guard = newGuard();
        when(systemConfigHelper.getAiChatPerUserMinute()).thenReturn(0);
        when(systemConfigHelper.getAiTokensPerUserDaily()).thenReturn(100000L);
        when(systemConfigHelper.getAiRoleTokenQuotas()).thenReturn(Map.of(1L, 0L));
        when(permissionService.getUserRoleIdListByUserId(USER_ID)).thenReturn(Set.of(1L));

        // 配额 0 = 不限制，check 不应查询 token 计数器即放行
        assertNull(guard.check(USER_ID));
        verify(redissonClient, never()).getAtomicLong(startsWith("ai:quota:tokens:"));
    }

    @Test
    @DisplayName("角色解析抛异常：回退兜底配额，不 fail-open 全放行")
    void resolveQuota_roleResolveThrows_fallback() {
        guard = newGuard();
        when(systemConfigHelper.getAiChatPerUserMinute()).thenReturn(0);
        when(systemConfigHelper.getAiTokensPerUserDaily()).thenReturn(100000L);
        when(systemConfigHelper.getAiRoleTokenQuotas()).thenReturn(Map.of(1L, 500000L));
        when(permissionService.getUserRoleIdListByUserId(USER_ID)).thenThrow(new RuntimeException("db down"));
        stubTokenCounter(100000L);

        // 角色解析失败回退兜底 100000，已用 100000 → 拒绝（而非放行）
        assertEquals("今日 AI 用量已达上限，明天再来吧", guard.check(USER_ID));
    }

    @Test
    @DisplayName("Redis 抛异常：fail-open 放行，无异常上抛")
    void check_redisThrows_failOpen() {
        guard = newGuard();
        when(systemConfigHelper.getAiChatPerUserMinute()).thenReturn(8);
        when(redissonClient.getAtomicLong(anyString())).thenThrow(new RuntimeException("redis down"));

        assertNull(guard.check(USER_ID));
    }

    @Test
    @DisplayName("recordUsage：配额开启时累加当日用量")
    void recordUsage_quotaOn_accumulates() {
        guard = newGuard();
        when(systemConfigHelper.getAiTokensPerUserDaily()).thenReturn(100000L);
        when(systemConfigHelper.getAiRoleTokenQuotas()).thenReturn(Map.of());
        RAtomicLong counter = mock(RAtomicLong.class);
        when(counter.addAndGet(1500L)).thenReturn(1500L);
        when(counter.remainTimeToLive()).thenReturn(-1L);
        when(redissonClient.getAtomicLong(contains("ai:quota:tokens:"))).thenReturn(counter);

        guard.recordUsage(USER_ID, 1500L);

        verify(counter).addAndGet(1500L);
        verify(counter).expire(java.time.Duration.ofHours(48));
    }

    @Test
    @DisplayName("recordUsage：配额关闭时空操作")
    void recordUsage_quotaOff_noop() {
        guard = newGuard();
        when(systemConfigHelper.getAiTokensPerUserDaily()).thenReturn(0L);
        when(systemConfigHelper.getAiRoleTokenQuotas()).thenReturn(Map.of());

        guard.recordUsage(USER_ID, 1500L);

        verify(redissonClient, never()).getAtomicLong(startsWith("ai:quota:tokens:"));
    }

    @Test
    @DisplayName("recordUsage：tokens<=0 时空操作")
    void recordUsage_nonPositive_noop() {
        guard = newGuard();

        guard.recordUsage(USER_ID, 0L);

        verify(redissonClient, never()).getAtomicLong(anyString());
        verify(systemConfigHelper, never()).getAiTokensPerUserDaily();
    }
}

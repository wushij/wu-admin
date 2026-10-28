package com.admin.server.modules.system.service.security;

import com.admin.server.framework.security.core.RiskPrincipal;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.admin.server.modules.system.service.security.impl.AntiBotRiskServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 分桶计分的真实 Redis 验证：Lua 脚本与 Redisson 返回值解码无法靠 mock 验证。
 *
 * <p>本机 127.0.0.1:6379 不可达时自动跳过，不影响没有 Redis 的 CI。
 * 只使用带随机后缀的测试主体，用后删除。</p>
 */
@DisplayName("风控分桶计分（真实 Redis）")
class AntiBotRiskServiceRedisTest {

    private static RedissonClient redisson;
    private static AntiBotRiskServiceImpl service;

    @BeforeAll
    static void setUp() {
        boolean reachable;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", 6379), 500);
            reachable = true;
        } catch (Exception e) {
            reachable = false;
        }
        Assumptions.assumeTrue(reachable, "本机无 Redis，跳过真实 Redis 验证");

        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        try {
            redisson = Redisson.create(config);
            redisson.getBucket("security:risk:test:ping").isExists();
        } catch (Exception e) {
            Assumptions.abort("Redis 不可用或需要认证，跳过: " + e.getMessage());
        }

        SystemConfigHelper helper = mock(SystemConfigHelper.class);
        when(helper.isRiskEnabled()).thenReturn(true);
        when(helper.getRiskWindowMinutes()).thenReturn(60);
        when(helper.getRiskNoticeScore()).thenReturn(30);
        when(helper.getRiskWarnScore()).thenReturn(50);
        when(helper.getRiskChallengeScore()).thenReturn(7000);
        when(helper.getRiskBlockScore()).thenReturn(9000);
        when(helper.getRiskBanScore()).thenReturn(12000);
        when(helper.getRiskBlockSeconds()).thenReturn(3600L);

        service = new AntiBotRiskServiceImpl();
        ReflectionTestUtils.setField(service, "redissonClient", redisson);
        ReflectionTestUtils.setField(service, "systemConfigHelper", helper);
        ReflectionTestUtils.setField(service, "securityEventService", mock(SecurityEventService.class));
        ReflectionTestUtils.setField(service, "riskAlertNotifier", mock(RiskAlertNotifier.class));
    }

    @AfterAll
    static void tearDown() {
        if (redisson != null) {
            redisson.shutdown();
        }
    }

    @Test
    @DisplayName("真实 Redis：分数能累加并可读回，事件数正确，解封重置后归零")
    void scoreAccumulatesAgainstRealRedis() {
        String ip = "203.0.113." + (UUID.randomUUID().hashCode() & 0xff) + "-t" + System.nanoTime();
        RiskPrincipal subject = RiskPrincipal.ofIp(ip);
        try {
            assertEquals(10, service.addRisk(subject, 10, "TEST", "WARNING", "/t", "GET", "ua", "r1"));
            assertEquals(35, service.addRisk(subject, 25, "TEST", "WARNING", "/t", "GET", "ua", "r2"));
            assertEquals(35, service.getRiskScore(subject.key()), "只读查询应与累加结果一致");
            assertTrue(service.getActiveRiskList().stream()
                    .anyMatch(vo -> subject.key().equals(vo.getPrincipal())
                            && vo.getRiskScore() == 35 && vo.getEventCount() == 2));

            service.unblockPrincipal(subject.key(), true);
            assertEquals(0, service.getRiskScore(subject.key()));
        } finally {
            service.unblockPrincipal(subject.key(), true);
        }
    }

    @Test
    @DisplayName("真实 Redis：封禁只延长不缩短")
    void blockOnlyExtendsAgainstRealRedis() {
        String key = "ip:198.51.100.t" + System.nanoTime();
        try {
            service.blockPrincipal(key, 7 * 86400L, "长封禁");
            service.blockPrincipal(key, 86400L, "蜜罐");
            long remain = redisson.getBucket("security:risk:block:" + key).remainTimeToLive();
            assertTrue(remain > 2 * 86400_000L, "长封禁不应被缩短，剩余毫秒=" + remain);
        } finally {
            service.unblockPrincipal(key, true);
        }
    }
}
package cn.rbac.server.modules.system.service.auth.impl;

import cn.rbac.server.modules.system.service.auth.vo.LoginLockStatusVO;
import cn.rbac.server.modules.system.service.config.SystemConfigHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginLockServiceImpl 单元测试")
class LoginLockServiceImplTest {

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private SystemConfigHelper systemConfigHelper;

    @InjectMocks
    private LoginLockServiceImpl loginLockService;

    @SuppressWarnings("unchecked")
    private RBucket<Object> mockBucket() {
        return mock(RBucket.class);
    }

    @Test
    @DisplayName("recordLoginFailure：账号与 IP 使用不同阈值")
    void recordLoginFailure_usesSeparateThresholds() {
        when(systemConfigHelper.getMaxRetryCount()).thenReturn(5);
        when(systemConfigHelper.getMaxRetryCountIp()).thenReturn(20);
        when(systemConfigHelper.getLockTimeMinutes()).thenReturn(10);

        RBucket<Object> userFailBucket = mockBucket();
        RBucket<Object> userLockBucket = mockBucket();
        RBucket<Object> ipFailBucket = mockBucket();
        RBucket<Object> ipLockBucket = mockBucket();
        when(redissonClient.getBucket("auth:login:fail:user:alice")).thenReturn(userFailBucket);
        when(redissonClient.getBucket("auth:login:lock:user:alice")).thenReturn(userLockBucket);
        when(redissonClient.getBucket("auth:login:fail:ip:127.0.0.1")).thenReturn(ipFailBucket);
        when(redissonClient.getBucket("auth:login:lock:ip:127.0.0.1")).thenReturn(ipLockBucket);
        when(userFailBucket.get()).thenReturn(4);
        when(ipFailBucket.get()).thenReturn(19);

        loginLockService.recordLoginFailure("alice", "127.0.0.1");

        verify(userFailBucket).set(eq(5), eq(Duration.ofMinutes(10)));
        verify(ipFailBucket).set(eq(20), eq(Duration.ofMinutes(10)));
        verify(userLockBucket).set(anyLong(), eq(Duration.ofMinutes(10)));
        verify(ipLockBucket).set(anyLong(), eq(Duration.ofMinutes(10)));
        verify(userFailBucket).delete();
        verify(ipFailBucket).delete();
    }

    @Test
    @DisplayName("getUserLockStatus：已锁定时返回剩余秒数")
    void getUserLockStatus_locked() {
        RBucket<Object> lockBucket = mockBucket();
        when(redissonClient.getBucket("auth:login:lock:user:alice")).thenReturn(lockBucket);
        when(lockBucket.get()).thenReturn(System.currentTimeMillis());
        when(lockBucket.remainTimeToLive()).thenReturn(125_000L);

        LoginLockStatusVO status = loginLockService.getUserLockStatus("alice");

        assertTrue(status.isLocked());
        assertEquals(125L, status.getRemainSeconds());
    }

    @Test
    @DisplayName("unlockIp：清除 fail 与 lock key")
    void unlockIp_clearsKeys() {
        RBucket<Object> failBucket = mockBucket();
        RBucket<Object> lockBucket = mockBucket();
        when(redissonClient.getBucket("auth:login:fail:ip:127.0.0.1")).thenReturn(failBucket);
        when(redissonClient.getBucket("auth:login:lock:ip:127.0.0.1")).thenReturn(lockBucket);

        loginLockService.unlockIp("127.0.0.1");

        verify(failBucket).delete();
        verify(lockBucket).delete();
    }

    @Test
    @DisplayName("unlockUser：清除 fail 与 lock key")
    void unlockUser_clearsKeys() {
        RBucket<Object> failBucket = mockBucket();
        RBucket<Object> lockBucket = mockBucket();
        when(redissonClient.getBucket("auth:login:fail:user:alice")).thenReturn(failBucket);
        when(redissonClient.getBucket("auth:login:lock:user:alice")).thenReturn(lockBucket);

        loginLockService.unlockUser("alice");

        verify(failBucket).delete();
        verify(lockBucket).delete();
    }
}

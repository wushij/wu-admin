package com.admin.server.modules.system.service.auth;

import com.admin.server.modules.system.service.config.SystemConfigHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SliderCaptchaService 单元测试")
class SliderCaptchaServiceTest {

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private SystemConfigHelper systemConfigHelper;

    @InjectMocks
    private SliderCaptchaService sliderCaptchaService;

    @Test
    @DisplayName("verifyAndConsume：拒绝 legacy slider_verified")
    void verifyAndConsume_rejectsLegacyBypass() {
        assertEquals("请完成滑块验证",
                sliderCaptchaService.verifyAndConsume("token", SliderCaptchaService.LEGACY_BYPASS_CODE));
    }

    @Test
    @DisplayName("verifyAndConsume：offset 匹配时消费 token")
    void verifyAndConsume_success() {
        @SuppressWarnings("unchecked")
        RBucket<Object> bucket = mock(RBucket.class);
        when(redissonClient.getBucket("captcha:slider:abc")).thenReturn(bucket);
        when(bucket.get()).thenReturn(180);

        assertNull(sliderCaptchaService.verifyAndConsume("abc", "182"));
        verify(bucket).delete();
    }

    @Test
    @DisplayName("verifyAndConsume：offset 偏差过大失败")
    void verifyAndConsume_mismatch() {
        @SuppressWarnings("unchecked")
        RBucket<Object> bucket = mock(RBucket.class);
        when(redissonClient.getBucket("captcha:slider:abc")).thenReturn(bucket);
        when(bucket.get()).thenReturn(180);

        assertEquals("滑块验证失败，请重试", sliderCaptchaService.verifyAndConsume("abc", "100"));
        verify(bucket, never()).delete();
    }

    @Test
    @DisplayName("createChallenge：返回 token 与拼图参数")
    void createChallenge_returnsPayload() {
        when(systemConfigHelper.isCaptchaEnabled()).thenReturn(true);
        when(systemConfigHelper.getCaptchaType()).thenReturn(SystemConfigHelper.CAPTCHA_TYPE_SLIDER);
        when(systemConfigHelper.getCaptchaPerIpMinute()).thenReturn(40);

        RAtomicLong counter = mock(RAtomicLong.class);
        when(counter.incrementAndGet()).thenReturn(1L);
        when(redissonClient.getAtomicLong(anyString())).thenReturn(counter);

        @SuppressWarnings("unchecked")
        RBucket<Object> bucket = mock(RBucket.class);
        when(redissonClient.getBucket(startsWith("captcha:slider:"))).thenReturn(bucket);

        Map<String, Object> result = sliderCaptchaService.createChallenge("login", "127.0.0.1");

        assertNotNull(result.get("token"));
        assertNotNull(result.get("targetX"));
        assertNotNull(result.get("bgIndex"));
        assertNotNull(result.get("pieceTop"));
        verify(bucket).set(any(), any());
    }
}

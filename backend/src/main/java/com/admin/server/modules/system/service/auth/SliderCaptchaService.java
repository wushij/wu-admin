package com.admin.server.modules.system.service.auth;

import cn.hutool.core.util.IdUtil;
import com.admin.server.common.pojo.BusinessException;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import jakarta.annotation.Resource;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 滑块验证码：服务端生成 challenge，提交时校验 offsetX 并一次性消费 token。
 */
@Service
public class SliderCaptchaService {

    /** 与前端 SliderCaptcha 组件对齐，禁止再接受该常量绕过 */
    public static final String LEGACY_BYPASS_CODE = "slider_verified";

    private static final String REDIS_KEY_PREFIX = "captcha:slider:";
    private static final int TOLERANCE = 6;
    private static final int PIECE_W = 52;
    private static final int PIECE_H = 52;
    private static final int IMAGE_H = 200;
    private static final int TARGET_MIN = 130;
    private static final int TARGET_RANGE = 110;
    /** 与 PC 滑块弹窗默认宽度一致，用于服务端约束 targetX 范围 */
    private static final int REFERENCE_WIDTH = 340;

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private SystemConfigHelper systemConfigHelper;

    public Map<String, Object> createChallenge(String scene, String clientIp) {
        if ("register".equalsIgnoreCase(scene)) {
            if (!systemConfigHelper.isRegisterCaptchaEnabled()
                    || !SystemConfigHelper.CAPTCHA_TYPE_SLIDER.equals(systemConfigHelper.getRegisterCaptchaType())) {
                throw new BusinessException(400, "当前未启用滑块验证码");
            }
        } else if (!systemConfigHelper.isCaptchaEnabled()
                || !SystemConfigHelper.CAPTCHA_TYPE_SLIDER.equals(systemConfigHelper.getCaptchaType())) {
            // login / profile / forgot 等场景：登录滑块未开时仍允许拉 challenge（如短信发码前滑块）
            if (!"sms".equalsIgnoreCase(scene) && !"profile".equalsIgnoreCase(scene)
                    && !"forgot".equalsIgnoreCase(scene)) {
                throw new BusinessException(400, "当前未启用滑块验证码");
            }
        }
        String rlMsg = rateLimitByIp(clientIp, "captcha", systemConfigHelper.getCaptchaPerIpMinute());
        if (rlMsg != null) {
            throw new BusinessException(429, rlMsg);
        }
        int maxOffset = Math.max(PIECE_W, REFERENCE_WIDTH - PIECE_W);
        int targetX = TARGET_MIN + ThreadLocalRandom.current().nextInt(TARGET_RANGE);
        targetX = Math.min(targetX, maxOffset - PIECE_W);
        int pieceTop = 28 + ThreadLocalRandom.current().nextInt(Math.max(1, IMAGE_H - PIECE_H - 56));
        int bgIndex = ThreadLocalRandom.current().nextInt(4);
        String token = IdUtil.simpleUUID();
        redissonClient.getBucket(REDIS_KEY_PREFIX + token)
                .set(targetX, Duration.ofMinutes(5));
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("bgIndex", bgIndex);
        result.put("pieceTop", pieceTop);
        result.put("targetX", targetX);
        return result;
    }

    /**
     * @return null 表示通过；否则为错误提示
     */
    public String verifyAndConsume(String token, String offsetXStr) {
        if (StringUtils.hasText(offsetXStr) && LEGACY_BYPASS_CODE.equals(offsetXStr.trim())) {
            return "请完成滑块验证";
        }
        if (!StringUtils.hasText(token)) {
            return "请完成滑块验证";
        }
        Integer offsetX = parseOffsetX(offsetXStr);
        if (offsetX == null) {
            return "请完成滑块验证";
        }
        RBucket<Integer> bucket = redissonClient.getBucket(REDIS_KEY_PREFIX + token.trim());
        Integer expected = bucket.get();
        if (expected == null) {
            return "滑块验证已过期，请刷新后重试";
        }
        if (Math.abs(expected - offsetX) > TOLERANCE) {
            return "滑块验证失败，请重试";
        }
        bucket.delete();
        return null;
    }

    private Integer parseOffsetX(String offsetXStr) {
        if (!StringUtils.hasText(offsetXStr)) {
            return null;
        }
        try {
            return Integer.parseInt(offsetXStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String rateLimitByIp(String clientIp, String action, int maxPerMinute) {
        if (maxPerMinute <= 0) {
            return null;
        }
        long minute = System.currentTimeMillis() / 60_000L;
        String redisKey = "auth:rl:" + action + ":" + clientIp + ":" + minute;
        RAtomicLong counter = redissonClient.getAtomicLong(redisKey);
        long n = counter.incrementAndGet();
        if (n == 1) {
            counter.expire(Duration.ofSeconds(90));
        }
        if (n > maxPerMinute) {
            return "请求过于频繁，请稍后再试";
        }
        return null;
    }
}

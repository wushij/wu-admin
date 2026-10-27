package com.admin.server.modules.system.service.auth;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.system.api.auth.vo.SliderTrackEventVO;
import com.admin.server.modules.system.service.auth.support.SliderImageRenderer;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import com.admin.server.framework.security.core.SliderChallengeVerifier;

/**
 * 滑块验证码：服务端生成带缺口的图片，答案仅保存在 Redis，校验通过后签发一次性票据。
 * <p>响应体不再包含 targetX，前端无法直接得到正确偏移量；提交时必须同时携带
 * 拖动轨迹和耗时，服务端完成行为校验后才会签发票据。</p>
 */
@Slf4j
@Service
public class SliderCaptchaService implements SliderChallengeVerifier {

    /** 历史遗留前端常量，禁止再作为通过凭证使用 */
    public static final String LEGACY_BYPASS_CODE = "slider_verified";

    private static final String CHALLENGE_KEY_PREFIX = "captcha:slider:challenge:";
    private static final String TICKET_KEY_PREFIX = "captcha:slider:ticket:";
    private static final String TICKET_PREFIX = "ct_slider_";

    private static final int TOLERANCE = 6;
    private static final int REFERENCE_WIDTH = SliderImageRenderer.WIDTH;
    private static final int IMAGE_HEIGHT = SliderImageRenderer.HEIGHT;
    private static final int PIECE_SIZE = SliderImageRenderer.PIECE_SIZE;
    private static final int TARGET_MIN = 130;
    private static final int TARGET_RANGE = 110;
    private static final int MAX_TARGET_X = REFERENCE_WIDTH - PIECE_SIZE * 2;
    private static final int MAX_FAIL_COUNT = 3;

    private static final int MIN_TRACK_EVENTS = 3;
    private static final int MAX_TRACK_EVENTS = 240;
    private static final int MIN_DURATION_MS = 150;
    private static final int MAX_DURATION_MS = 20_000;
    private static final int MIN_TRACK_SPAN = 20;
    private static final int TRACK_END_TOLERANCE = 15;

    private static final Set<String> ALLOWED_SCENES = Set.of(
            "login", "register", "sms", "email", "profile", "forgot");

    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private static final Duration TICKET_TTL = Duration.ofMinutes(2);

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private SystemConfigHelper systemConfigHelper;

    @Resource
    private SliderImageRenderer sliderImageRenderer;

    @Resource
    private com.admin.server.modules.system.service.security.AntiBotRiskService antiBotRiskService;

    /**
     * 生成滑块挑战：返回服务端渲染的图片、拼图块和 Y 坐标，不返回缺口 X 坐标。
     */
    public Map<String, Object> createChallenge(String scene, String clientIp) {
        validateSceneEnabled(scene);

        String rlMsg = rateLimitByIp(clientIp, "captcha", systemConfigHelper.getCaptchaPerIpMinute());
        if (rlMsg != null) {
            throw new BusinessException(429, rlMsg);
        }

        int targetX = TARGET_MIN + ThreadLocalRandom.current().nextInt(TARGET_RANGE);
        targetX = Math.min(targetX, MAX_TARGET_X);
        int pieceTop = 28 + ThreadLocalRandom.current().nextInt(
                Math.max(1, IMAGE_HEIGHT - PIECE_SIZE - 56));
        int bgIndex = ThreadLocalRandom.current().nextInt(SliderImageRenderer.BG_COUNT);

        SliderImageRenderer.RenderedImage rendered =
                sliderImageRenderer.render(bgIndex, targetX, pieceTop);

        String token = IdUtil.simpleUUID();
        ChallengeState state = new ChallengeState();
        state.setTargetX(targetX);
        state.setPieceTop(pieceTop);
        state.setScene(scene);
        state.setClientIp(clientIp);
        state.setFailCount(0);
        state.setCreatedAt(System.currentTimeMillis());
        redissonClient.getBucket(CHALLENGE_KEY_PREFIX + token)
                .set(JSONUtil.toJsonStr(state), CHALLENGE_TTL);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("background", rendered.background());
        result.put("piece", rendered.piece());
        result.put("width", REFERENCE_WIDTH);
        result.put("height", IMAGE_HEIGHT);
        result.put("pieceSize", PIECE_SIZE);
        result.put("pieceTop", pieceTop);
        result.put("expireAt", System.currentTimeMillis() + CHALLENGE_TTL.toMillis());
        return result;
    }

    /**
     * 校验滑块并签发一次性票据。
     * <p>校验项：拖动轨迹、耗时、轨迹终点与提交偏移量的一致性、偏移量与缺口位置的误差。
     * 同一 challenge 连续失败 3 次即作废，必须重新获取图片。</p>
     */
    public Map<String, Object> verifyChallenge(String token, Integer offsetX, Integer durationMs,
                                               List<SliderTrackEventVO> events, String clientIp) {
        if (StrUtil.isBlank(token)) {
            throw new BusinessException(400, "验证已过期，请刷新后重试");
        }
        if (offsetX == null || offsetX < 0 || offsetX > REFERENCE_WIDTH) {
            throw new BusinessException(400, "滑块参数非法");
        }

        String challengeKey = CHALLENGE_KEY_PREFIX + token.trim();
        RBucket<String> challengeBucket = redissonClient.getBucket(challengeKey);
        String raw = challengeBucket.get();
        if (StrUtil.isBlank(raw)) {
            throw new BusinessException(400, "验证已过期，请刷新后重试");
        }

        ChallengeState state;
        try {
            state = JSONUtil.toBean(raw, ChallengeState.class);
        } catch (Exception e) {
            challengeBucket.delete();
            throw new BusinessException(400, "验证数据异常，请刷新后重试");
        }
        if (state.getTargetX() == null) {
            challengeBucket.delete();
            throw new BusinessException(400, "验证数据异常，请刷新后重试");
        }
        // challenge 与获取它的客户端 IP 绑定，避免验证码被转发给打码平台远程代答
        if (StrUtil.isNotBlank(state.getClientIp()) && StrUtil.isNotBlank(clientIp)
                && !state.getClientIp().equals(clientIp)) {
            challengeBucket.delete();
            throw new BusinessException(400, "验证已失效，请刷新后重试");
        }

        String behaviorError = validateBehavior(offsetX, durationMs, events);
        if (behaviorError != null) {
            recordFailure(challengeKey, state, clientIp, behaviorError);
        }
        if (Math.abs(state.getTargetX() - offsetX) > TOLERANCE) {
            recordFailure(challengeKey, state, clientIp, "滑块验证失败，请重试");
        }

        challengeBucket.delete();
        // 验证成功后清理失败计数，避免同一 challengeKey 的计数残留
        redissonClient.getAtomicLong(challengeKey + ":fail").delete();

        String ticket = TICKET_PREFIX + IdUtil.simpleUUID();
        TicketState ticketState = new TicketState();
        ticketState.setScene(state.getScene());
        ticketState.setClientIp(clientIp);
        ticketState.setCreatedAt(System.currentTimeMillis());
        redissonClient.getBucket(TICKET_KEY_PREFIX + ticket)
                .set(JSONUtil.toJsonStr(ticketState), TICKET_TTL);

        Map<String, Object> result = new HashMap<>();
        result.put("ticket", ticket);
        result.put("expireAt", System.currentTimeMillis() + TICKET_TTL.toMillis());
        return result;
    }

    /**
     * 消费一次性滑块票据（兼容旧调用：不校验场景与 IP）。
     *
     * @return null 表示通过；否则为错误提示
     */
    public String verifyAndConsume(String token, String code) {
        return verifyAndConsume(token, code, null, null);
    }

    /**
     * 消费一次性滑块票据，并校验票据绑定的业务场景与客户端 IP。
     * <p>票据由 {@link #verifyChallenge} 签发，scene/clientIp 在签发时写入 Redis。
     * 不校验会导致「登录场景票据可用于注册/找回密码」，也会让票据被远程代答后跨 IP 使用。</p>
     *
     * @param scene    期望的业务场景，null 表示不校验（仅用于兼容旧调用）
     * @param clientIp 当前请求客户端 IP，null 表示不校验
     * @return null 表示通过；否则为错误提示
     */
    public String verifyAndConsume(String token, String code, String scene, String clientIp) {
        if (StringUtils.hasText(code) && LEGACY_BYPASS_CODE.equals(code.trim())) {
            return "请完成滑块验证";
        }
        if (!StringUtils.hasText(token)) {
            return "请完成滑块验证";
        }
        String normalized = token.trim();
        if (!normalized.startsWith(TICKET_PREFIX)) {
            // 旧版直接把 challenge token + offsetX 当凭证，已不再接受
            return "滑块验证已过期，请重新验证";
        }

        RBucket<String> bucket = redissonClient.getBucket(TICKET_KEY_PREFIX + normalized);
        String raw;
        try {
            raw = bucket.getAndDelete();
        } catch (Exception e) {
            // 兼容不支持 GETDEL 的旧 Redis：退化为先读后删，票据短时效且仅能使用一次
            raw = bucket.get();
            bucket.delete();
        }
        if (StrUtil.isBlank(raw)) {
            return "滑块验证已过期，请重新验证";
        }

        TicketState ticketState;
        try {
            ticketState = JSONUtil.toBean(raw, TicketState.class);
        } catch (Exception e) {
            return "滑块验证已过期，请重新验证";
        }
        if (StrUtil.isNotBlank(scene) && StrUtil.isNotBlank(ticketState.getScene())
                && !scene.equalsIgnoreCase(ticketState.getScene())) {
            return "安全验证已失效，请重新验证";
        }
        if (StrUtil.isNotBlank(clientIp) && StrUtil.isNotBlank(ticketState.getClientIp())
                && !clientIp.equals(ticketState.getClientIp())) {
            return "安全验证已失效，请重新验证";
        }
        return null;
    }

    private void validateSceneEnabled(String scene) {
        if (StrUtil.isBlank(scene)
                || !ALLOWED_SCENES.contains(scene.trim().toLowerCase(Locale.ROOT))) {
            throw new BusinessException(400, "不支持的验证场景");
        }
        if ("register".equalsIgnoreCase(scene)) {
            if (!systemConfigHelper.isRegisterCaptchaEnabled()
                    || !SystemConfigHelper.CAPTCHA_TYPE_SLIDER.equals(systemConfigHelper.getRegisterCaptchaType())) {
                throw new BusinessException(400, "当前未启用滑块验证码");
            }
            return;
        }
        if ("profile".equalsIgnoreCase(scene)) {
            // 个人中心属于登录后的敏感操作，禁止匿名用户获取 profile 场景票据后跨场景复用
            if (!isLoggedIn()) {
                throw new BusinessException(403, "请先登录后再进行安全验证");
            }
            return;
        }
        if ("forgot".equalsIgnoreCase(scene)) {
            // 忘记密码本身就是未登录场景，保留强制滑块；票据绑定 scene 后不能用于登录/注册
            return;
        }
        if (systemConfigHelper.isCaptchaEnabled()
                && SystemConfigHelper.CAPTCHA_TYPE_SLIDER.equals(systemConfigHelper.getCaptchaType())) {
            return;
        }
        boolean codeSliderAllowed =
                ("sms".equalsIgnoreCase(scene) && systemConfigHelper.isSmsLoginSliderCaptchaEnabled())
                        || ("email".equalsIgnoreCase(scene) && systemConfigHelper.isEmailLoginSliderCaptchaEnabled());
        if (!codeSliderAllowed) {
            throw new BusinessException(400, "当前未启用滑块验证码");
        }
    }

    private boolean isLoggedIn() {
        try {
            return StpUtil.isLogin();
        } catch (Exception e) {
            return false;
        }
    }

    private void recordFailure(String challengeKey, ChallengeState state, String clientIp, String message) {
        // 使用 Redis 原子自增，避免并发请求同时读取旧 failCount 后回写导致失败次数被绕过
        String failKey = challengeKey + ":fail";
        RAtomicLong failCounter = redissonClient.getAtomicLong(failKey);
        long failCount = failCounter.incrementAndGet();
        if (failCount == 1L) {
            failCounter.expire(CHALLENGE_TTL);
        }
        state.setFailCount((int) Math.min(failCount, Integer.MAX_VALUE));
        // 滑块失败计入行为风险分：单次 3 分，连续失败作废时额外 10 分。
        // 不阻塞当前校验流程，失败记录本身不能影响验证码的正常拒绝语义。
        try {
            boolean abuse = failCount >= MAX_FAIL_COUNT;
            antiBotRiskService.addRisk(clientIp, clientIp, abuse ? 13 : 3,
                    abuse ? "CAPTCHA_ABUSE" : "CAPTCHA_FAIL", "WARNING",
                    "/auth/slider-verify", "POST", "",
                    (abuse ? "滑块连续失败作废 challenge" : "滑块验证失败") + ": scene=" + state.getScene());
        } catch (Exception e) {
            log.debug("滑块失败风险分记录跳过: ip={}, error={}", clientIp, e.getMessage());
        }
        if (failCount >= MAX_FAIL_COUNT) {
            redissonClient.getBucket(challengeKey).delete();
            failCounter.delete();
            log.warn("滑块验证连续失败已作废 challenge: ip={}, scene={}, failCount={}",
                    clientIp, state.getScene(), failCount);
            throw new BusinessException(400, "验证失败次数过多，请刷新后重试");
        }
        redissonClient.getBucket(challengeKey).set(JSONUtil.toJsonStr(state), CHALLENGE_TTL);
        throw new BusinessException(400, message);
    }

    /**
     * 行为轨迹校验：拒绝没有拖动过程、耗时异常或轨迹方向明显不合理的请求。
     * <p>轨迹校验不能替代图像识别，但可以显著抬高脚本伪造请求的成本。</p>
     */
    private String validateBehavior(Integer offsetX, Integer durationMs, List<SliderTrackEventVO> events) {
        if (events == null || events.size() < MIN_TRACK_EVENTS || events.size() > MAX_TRACK_EVENTS) {
            return "行为轨迹异常，请重新验证";
        }
        if (durationMs == null || durationMs < MIN_DURATION_MS || durationMs > MAX_DURATION_MS) {
            return "操作时间异常，请重新验证";
        }

        int lastT = -1;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        for (SliderTrackEventVO event : events) {
            if (event == null || event.getX() == null || event.getT() == null) {
                return "行为轨迹异常，请重新验证";
            }
            if (event.getT() < lastT) {
                return "行为轨迹异常，请重新验证";
            }
            lastT = event.getT();
            minX = Math.min(minX, event.getX());
            maxX = Math.max(maxX, event.getX());
        }

        int nonMonotonic = 0;
        for (int i = 1; i < events.size(); i++) {
            if (events.get(i).getX() < events.get(i - 1).getX()) {
                nonMonotonic++;
            }
        }
        if (nonMonotonic > Math.max(1, events.size() / 4)) {
            return "行为轨迹异常，请重新验证";
        }
        if (maxX - minX < MIN_TRACK_SPAN) {
            return "行为轨迹异常，请重新验证";
        }

        // 允许用户拖过头后回拉，因此只要求最后一个采样点与最终提交位置一致
        SliderTrackEventVO last = events.get(events.size() - 1);
        if (Math.abs(last.getX() - offsetX) > TRACK_END_TOLERANCE) {
            return "行为轨迹与提交位置不一致，请重新验证";
        }
        if (Math.abs(last.getT() - durationMs) > 100) {
            return "行为轨迹异常，请重新验证";
        }
        return null;
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

    @Data
    @NoArgsConstructor
    public static class ChallengeState {
        private Integer targetX;
        private Integer pieceTop;
        private String scene;
        private String clientIp;
        private Integer failCount;
        private Long createdAt;
    }

    @Data
    @NoArgsConstructor
    public static class TicketState {
        private String scene;
        private String clientIp;
        private Long createdAt;
    }
}


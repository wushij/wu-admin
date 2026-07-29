package com.admin.server.modules.ai.service.impl;

import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.admin.server.modules.system.service.permission.PermissionService;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * AI 对话限流与 Token 配额守卫（设计方案 §3 三道闸门 · §4 详细设计）
 * <p>
 * 闸门①每用户每分钟次数（固定窗口，复用既有限流模式）；闸门②每用户每日 token 配额
 * （角色级分配，多角色取最大值，未命中落兜底，0=不限制）。计数始终按 userId，角色只决定
 * 配额上限值。全程 fail-open：任何异常（含 Redis 不可用）一律放行，仅记 warn，限流不能
 * 成为对话可用性的单点。
 * </p>
 */
@Component
public class AiChatQuotaGuard {

    private static final Logger log = LoggerFactory.getLogger(AiChatQuotaGuard.class);

    /** 分钟级限流 key 前缀（ai:rl:chat:{userId}:{minute}） */
    private static final String MINUTE_KEY_PREFIX = "ai:rl:chat:";
    /** 日 token 配额 key 前缀（ai:quota:tokens:{userId}:{yyyyMMdd}） */
    private static final String TOKEN_QUOTA_KEY_PREFIX = "ai:quota:tokens:";

    private static final String MSG_TOO_FREQUENT = "提问太频繁了，请稍候再试 🍵";
    private static final String MSG_QUOTA_EXCEEDED = "今日 AI 用量已达上限，明天再来吧";

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private SystemConfigHelper systemConfigHelper;
    @Resource
    private PermissionService permissionService;

    /**
     * 依次通过闸门①②；通过返回 null，被拒返回可回传前端的中文文案。
     * 内部完成分钟计数自增；任何异常 fail-open 放行。
     */
    public String check(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }
        try {
            String freqErr = checkPerMinute(userId);
            if (freqErr != null) {
                return freqErr;
            }
            return checkDailyTokenQuota(userId);
        } catch (Exception e) {
            log.warn("AI 对话配额检查异常，fail-open 放行 user={}: {}", userId, e.getMessage());
            return null;
        }
    }

    /** 闸门①：分钟级固定窗口计数，进门即自增 */
    private String checkPerMinute(Long userId) {
        int maxPerMinute = systemConfigHelper.getAiChatPerUserMinute();
        if (maxPerMinute <= 0) {
            return null;
        }
        long minute = System.currentTimeMillis() / 60_000L;
        RAtomicLong counter = redissonClient.getAtomicLong(MINUTE_KEY_PREFIX + userId + ":" + minute);
        long n = counter.incrementAndGet();
        if (n == 1) {
            counter.expire(Duration.ofMinutes(2));
        }
        return n > maxPerMinute ? MSG_TOO_FREQUENT : null;
    }

    /** 闸门②：日 token 配额（不预扣，宽进严出） */
    private String checkDailyTokenQuota(Long userId) {
        long quota = resolveDailyQuota(userId);
        if (quota <= 0) {
            // 配额关闭或该用户不限量
            return null;
        }
        long used = redissonClient.getAtomicLong(tokenQuotaKey(userId)).get();
        return used >= quota ? MSG_QUOTA_EXCEEDED : null;
    }

    /**
     * 回写当日 token 用量（onComplete 调用，成功与中断均回写）。
     * tokens<=0 或配额整体关闭时空操作；任何异常仅 warn，不影响主流程。
     */
    public void recordUsage(Long userId, long totalTokens) {
        if (userId == null || userId <= 0 || totalTokens <= 0) {
            return;
        }
        try {
            if (resolveDailyQuota(userId) <= 0) {
                // 配额关闭：无需累计，省一次 Redis 写入
                return;
            }
            RAtomicLong counter = redissonClient.getAtomicLong(tokenQuotaKey(userId));
            counter.addAndGet(totalTokens);
            if (counter.remainTimeToLive() < 0) {
                // 首次写入（或历史无 TTL）时补设过期，跨日由 key 中的日期自然切换
                counter.expire(Duration.ofHours(48));
            }
        } catch (Exception e) {
            log.warn("AI 对话 token 用量回写异常 user={}: {}", userId, e.getMessage());
        }
    }

    /**
     * 解析用户当日 token 配额（§4.3）：
     * 角色规则多命中取最大值，命中配 0 表示该角色不限量（返回 0 短路）；
     * 一条未命中（含角色被删除）落兜底 {@code tokensPerUserDaily}；角色解析异常回退兜底值（不 fail-open 全放行）。
     * 返回 0 表示不限制 / 闸门关闭。
     */
    private long resolveDailyQuota(Long userId) {
        long fallback = systemConfigHelper.getAiTokensPerUserDaily();
        Map<Long, Long> roleQuotas = systemConfigHelper.getAiRoleTokenQuotas();
        if (roleQuotas == null || roleQuotas.isEmpty()) {
            return fallback;
        }
        Set<Long> roleIds;
        try {
            roleIds = permissionService.getUserRoleIdListByUserId(userId);
        } catch (Exception e) {
            log.warn("AI 配额角色解析失败，回退兜底配额 user={}: {}", userId, e.getMessage());
            return fallback;
        }
        if (roleIds == null || roleIds.isEmpty()) {
            return fallback;
        }
        boolean matched = false;
        long max = 0L;
        for (Long roleId : roleIds) {
            Long q = roleQuotas.get(roleId);
            if (q == null) {
                continue;
            }
            matched = true;
            if (q <= 0) {
                // 该角色不限量，0 语义优先于任何数值上限
                return 0L;
            }
            max = Math.max(max, q);
        }
        return matched ? max : fallback;
    }

    private String tokenQuotaKey(Long userId) {
        return TOKEN_QUOTA_KEY_PREFIX + userId + ":" + LocalDate.now();
    }
}

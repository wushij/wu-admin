package com.admin.server.modules.system.service.security.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.admin.server.modules.system.api.security.vo.RiskSubjectVO;
import com.admin.server.modules.system.dal.dataobject.security.SecurityEventDO;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.admin.server.modules.system.service.security.AntiBotRiskService;
import com.admin.server.modules.system.service.security.RiskAlertNotifier;
import com.admin.server.modules.system.service.security.RiskLevel;
import com.admin.server.framework.security.core.RiskPrincipal;
import com.admin.server.modules.system.service.security.RiskSubjectPage;
import com.admin.server.modules.system.service.security.SecurityEventService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.redisson.api.options.KeysScanOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * 行为风控与反爬防御核心服务实现。
 *
 * <p>风险分使用 Redis 有序集合实现滑动窗口：成员内容是「分值|随机串」，score 是事件毫秒时间戳。
 * 读取时只汇总窗口内的成员，过期事件不再计入，因此分数会随攻击行为停止而自然回落，
 * 不需要额外的衰减任务。</p>
 */
@Slf4j
@Service
public class AntiBotRiskServiceImpl implements AntiBotRiskService {

    /** 风险分窗口前缀，成员为「分值|随机串」，score 为事件时间戳 */
    private static final String RISK_WINDOW_PREFIX = "security:risk:z:";

    /** 封禁标记前缀，value 为封禁原因 */
    private static final String BLOCK_PREFIX = "security:risk:block:";

    /** 挑战通过后的豁免前缀 */
    private static final String CHALLENGE_PASS_PREFIX = "security:risk:pass:";

    /** 分值字段分隔符 */
    private static final String MEMBER_SEP = "|";

    /** 单次扫描的最大主体数量，防止 Redis 键空间异常时拖慢管理接口 */
    private static final int SCAN_LIMIT = 1000;

    /**
     * 自动化爬虫、漏洞扫描器与探测工具的 User-Agent 特征指纹库。
     *
     * <p>优先于签名校验执行，因此在任何放行路径上都能生效。</p>
     */
    private static final List<String> BLOCKED_USER_AGENTS = List.of(
            "python-requests",
            "curl/",
            "wget/",
            "scrapy/",
            "go-http-client",
            "aiohttp/",
            "httpx/",
            "libwww-perl",
            "headlesschrome",
            "phantomjs",
            "selenium",
            "puppeteer",
            "playwright",
            "sqlmap",
            "nikto",
            "nmap",
            "masscan",
            "zgrab",
            "gobuster",
            "ffuf",
            "wfuzz",
            "dirbuster"
    );

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private SecurityEventService securityEventService;

    @Resource
    private SystemConfigHelper systemConfigHelper;

    @Resource
    private RiskAlertNotifier riskAlertNotifier;

    // ---------------------------------------------------------------- 计分

    @Override
    public int addRisk(String principal, String ip, int delta, String eventType, String action,
                       String uri, String method, String userAgent, String reason) {
        return addRisk(RiskPrincipal.normalize(principal, ip), delta, eventType, action,
                uri, method, userAgent, reason);
    }

    @Override
    public int addRisk(RiskPrincipal subject, int delta, String eventType, String action,
                       String uri, String method, String userAgent, String reason) {
        if (subject == null) {
            return 0;
        }
        if (!systemConfigHelper.isRiskEnabled()) {
            return getRiskScore(subject.key());
        }

        int windowMinutes = systemConfigHelper.getRiskWindowMinutes();
        int total = touchScore(subject, delta, windowMinutes);

        // 登录主体额外给 IP 记一半分：既保留「换 IP 也逃不掉账号维度」的能力，
        // 又不至于让同一个 NAT 出口被单个用户的异常行为直接顶到封禁阈值。
        if (subject.isUser() && StrUtil.isNotBlank(subject.ip()) && delta > 0) {
            touchScore(RiskPrincipal.ofIp(subject.ip()), Math.max(1, delta / 2), windowMinutes);
        }

        int warnScore = systemConfigHelper.getRiskWarnScore();
        int challengeScore = systemConfigHelper.getRiskChallengeScore();
        int blockScore = systemConfigHelper.getRiskBlockScore();
        int banScore = systemConfigHelper.getRiskBanScore();
        RiskLevel level = RiskLevel.of(total, systemConfigHelper.getRiskNoticeScore(),
                warnScore, challengeScore, blockScore);

        String effectiveAction = StrUtil.blankToDefault(action, "WARNING");
        boolean explicitForbidden = "FORBIDDEN".equalsIgnoreCase(action);
        if (total >= banScore) {
            String blockReason = "风险分达 " + total + "，触发自动封禁";
            applyBlock(subject, systemConfigHelper.getRiskBanSeconds(), blockReason, true);
            effectiveAction = "FORBIDDEN";
        } else if (total >= blockScore) {
            String blockReason = "风险分达 " + total + "，触发自动临时封禁";
            applyBlock(subject, systemConfigHelper.getRiskBlockSeconds(), blockReason, false);
            effectiveAction = "FORBIDDEN";
        } else if (total >= challengeScore && !explicitForbidden) {
            effectiveAction = "CHALLENGE";
        } else if (total >= warnScore && !explicitForbidden
                && !"BLOCKED".equalsIgnoreCase(action)) {
            effectiveAction = "WARNING";
        }

        persistEvent(subject, delta, eventType, effectiveAction, uri, method, userAgent, reason, total);

        boolean needAlert = "FORBIDDEN".equals(effectiveAction)
                || level.getCode() >= RiskLevel.CHALLENGE.getCode();
        if (needAlert) {
            riskAlertNotifier.notify(eventType, subject.key(), level.getText(), total,
                    effectiveAction, reason, uri, StrUtil.blankToDefault(subject.ip(), ""));
        }
        return total;
    }

    /**
     * 累加一次风险事件，返回窗口内的总分。
     */
    private int touchScore(RiskPrincipal subject, int delta, int windowMinutes) {
        long now = System.currentTimeMillis();
        long from = now - windowMinutes * 60_000L;
        RScoredSortedSet<String> set = redissonClient.getScoredSortedSet(RISK_WINDOW_PREFIX + subject.key());
        try {
            if (delta > 0) {
                set.add(now, delta + MEMBER_SEP + IdUtil.fastSimpleUUID());
                set.expire(Duration.ofMinutes(windowMinutes + 30L));
            }
            // 顺手清理窗口外的成员，避免集合无限增长
            set.removeRangeByScore(Double.NEGATIVE_INFINITY, true, from, false);
        } catch (Exception e) {
            log.warn("风险分写入失败: principal={}, error={}", subject.key(), e.getMessage());
            return 0;
        }
        return sumRange(set, from, now);
    }

    private int sumRange(RScoredSortedSet<String> set, long fromMillis, long toMillis) {
        Collection<ScoredEntry<String>> entries =
                set.entryRange((double) fromMillis, true, (double) toMillis, true);
        int sum = 0;
        for (ScoredEntry<String> entry : entries) {
            String value = entry.getValue();
            if (value == null) {
                continue;
            }
            int idx = value.indexOf(MEMBER_SEP);
            if (idx <= 0) {
                continue;
            }
            try {
                sum += Integer.parseInt(value.substring(0, idx));
            } catch (NumberFormatException ignored) {
                // 成员损坏时跳过，不能让一条脏数据拖垮整次计分
            }
        }
        return sum;
    }

    // ---------------------------------------------------------------- 查询

    @Override
    public int getRiskScore(String principal) {
        RiskPrincipal subject = RiskPrincipal.normalize(principal, null);
        return scoreOf(subject.key());
    }

    @Override
    public RiskLevel getRiskLevel(String principal) {
        return levelOf(getRiskScore(principal));
    }

    private RiskLevel levelOf(int score) {
        return RiskLevel.of(score,
                systemConfigHelper.getRiskNoticeScore(),
                systemConfigHelper.getRiskWarnScore(),
                systemConfigHelper.getRiskChallengeScore(),
                systemConfigHelper.getRiskBlockScore());
    }

    private int scoreOf(String principalKey) {
        int windowMinutes = systemConfigHelper.getRiskWindowMinutes();
        long now = System.currentTimeMillis();
        long from = now - windowMinutes * 60_000L;
        RScoredSortedSet<String> set = redissonClient.getScoredSortedSet(RISK_WINDOW_PREFIX + principalKey);
        try {
            return sumRange(set, from, now);
        } catch (Exception e) {
            log.debug("风险分读取失败: principal={}, error={}", principalKey, e.getMessage());
            return 0;
        }
    }

    // ---------------------------------------------------------------- 封禁

    @Override
    public boolean isBlocked(String ip) {
        RiskPrincipal subject = RiskPrincipal.normalize(ip, ip);
        if (isBlocked(subject)) {
            return true;
        }
        // 兼容升级前写入的裸 IP 封禁键，避免上线瞬间把正在封禁的攻击者放出来
        if (StrUtil.isNotBlank(ip)) {
            try {
                return redissonClient.getBucket(BLOCK_PREFIX + ip.trim()).isExists();
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean isBlocked(RiskPrincipal subject) {
        if (subject == null || StrUtil.isBlank(subject.key())) {
            return false;
        }
        try {
            return redissonClient.getBucket(BLOCK_PREFIX + subject.key()).isExists();
        } catch (Exception e) {
            // 风控存储故障时按放行处理，优先保证站点可用
            log.warn("读取封禁状态失败: principal={}, error={}", subject.key(), e.getMessage());
            return false;
        }
    }

    @Override
    public void blockPrincipal(String principal, long durationSeconds, String reason) {
        if (StrUtil.isBlank(principal)) {
            return;
        }
        RiskPrincipal subject = RiskPrincipal.normalize(principal, principal);
        long seconds = Math.max(60, durationSeconds);
        try {
            redissonClient.getBucket(BLOCK_PREFIX + subject.key())
                    .set(StrUtil.blankToDefault(reason, "网络环境异常被系统风控封禁"),
                            Duration.ofSeconds(seconds));
            log.warn("风险主体已被封禁: principal={}, durationSeconds={}, reason={}",
                    subject.key(), seconds, reason);
        } catch (Exception e) {
            log.error("封禁写入失败: principal={}, error={}", subject.key(), e.getMessage());
        }
    }

    @Override
    public void unblockPrincipal(String principal, boolean resetRisk) {
        if (StrUtil.isBlank(principal)) {
            return;
        }
        RiskPrincipal subject = RiskPrincipal.normalize(principal, principal);
        try {
            redissonClient.getBucket(BLOCK_PREFIX + subject.key()).delete();
            // 清掉可能残留的裸 IP 封禁键
            redissonClient.getBucket(BLOCK_PREFIX + principal.trim()).delete();
            if (resetRisk) {
                redissonClient.getScoredSortedSet(RISK_WINDOW_PREFIX + subject.key()).delete();
            }
            log.info("风险主体已解封: principal={}, resetRisk={}", subject.key(), resetRisk);
        } catch (Exception e) {
            log.error("解封失败: principal={}, error={}", subject.key(), e.getMessage());
        }
    }

    /**
     * 执行封禁：账号主体同时踢下线；只有最高档才连带封 IP。
     *
     * <p>账号级封禁由登录过滤器拦截，不默认连带封 IP，
     * 避免一个同事的异常操作把整个办公网出口封掉。</p>
     */
    private void applyBlock(RiskPrincipal subject, long seconds, String reason, boolean blockIpToo) {
        blockPrincipal(subject.key(), seconds, reason);
        if (!subject.isUser()) {
            return;
        }
        forceLogout(subject.userId());
        if (blockIpToo && StrUtil.isNotBlank(subject.ip())) {
            blockPrincipal(subject.ip(), seconds, reason);
        }
    }

    private void forceLogout(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            StpUtil.logout(userId);
            log.warn("风险账号已强制下线: userId={}", userId);
        } catch (Exception e) {
            log.debug("风险账号强制下线失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    // ---------------------------------------------------------------- 列表

    @Override
    public List<RiskSubjectVO> getActiveRiskList() {
        return collectSubjects(null);
    }

    @Override
    public RiskSubjectPage getRiskSubjectPage(int pageNo, int pageSize, String keyword,
                                               Integer level, Boolean blocked) {
        int safePageNo = Math.max(1, pageNo);
        int safePageSize = Math.max(1, Math.min(pageSize, 200));
        List<RiskSubjectVO> all = collectSubjects(keyword);
        List<RiskSubjectVO> filtered = new ArrayList<>(all.size());
        for (RiskSubjectVO vo : all) {
            if (level != null && !level.equals(vo.getRiskLevel())) {
                continue;
            }
            if (blocked != null && !blocked.equals(vo.getBlocked())) {
                continue;
            }
            filtered.add(vo);
        }
        int total = filtered.size();
        int from = (safePageNo - 1) * safePageSize;
        List<RiskSubjectVO> records;
        if (from >= total) {
            records = List.of();
        } else {
            records = filtered.subList(from, Math.min(from + safePageSize, total));
        }
        return new RiskSubjectPage(records, total, safePageNo, safePageSize);
    }

    private List<RiskSubjectVO> collectSubjects(String keyword) {
        List<RiskSubjectVO> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        int windowMinutes = systemConfigHelper.getRiskWindowMinutes();
        long from = now - windowMinutes * 60_000L;
        String lowerKeyword = StrUtil.isBlank(keyword) ? null : keyword.trim().toLowerCase();

        RKeys rkeys = redissonClient.getKeys();
        Iterable<String> keyNames = rkeys.getKeys(KeysScanOptions.defaults()
                .pattern(RISK_WINDOW_PREFIX + "*"));
        int scanned = 0;
        for (String name : keyNames) {
            if (scanned++ >= SCAN_LIMIT) {
                break;
            }
            if (StrUtil.isBlank(name) || name.length() <= RISK_WINDOW_PREFIX.length()) {
                continue;
            }
            String principalKey = name.substring(RISK_WINDOW_PREFIX.length());
            if (lowerKeyword != null && !principalKey.toLowerCase().contains(lowerKeyword)) {
                continue;
            }
            try {
                RScoredSortedSet<String> set = redissonClient.getScoredSortedSet(name);
                Collection<ScoredEntry<String>> entries =
                        set.entryRange((double) from, true, (double) now, true);
                if (entries.isEmpty()) {
                    continue;
                }
                RiskSubjectVO vo = buildSubjectVO(principalKey, entries);
                if (vo.getRiskScore() > 0 || Boolean.TRUE.equals(vo.getBlocked())) {
                    list.add(vo);
                }
            } catch (Exception e) {
                log.debug("读取风险主体失败: key={}, error={}", name, e.getMessage());
            }
        }
        list.sort(Comparator.comparing(RiskSubjectVO::getRiskScore,
                Comparator.nullsLast(Comparator.<Integer>reverseOrder())));
        return list;
    }

    private RiskSubjectVO buildSubjectVO(String principalKey, Collection<ScoredEntry<String>> entries) {
        int score = 0;
        long latest = 0L;
        for (ScoredEntry<String> entry : entries) {
            String value = entry.getValue();
            if (value != null) {
                int idx = value.indexOf(MEMBER_SEP);
                if (idx > 0) {
                    try {
                        score += Integer.parseInt(value.substring(0, idx));
                    } catch (NumberFormatException ignored) {
                        // 单条脏数据不影响整体
                    }
                }
            }
            latest = Math.max(latest, entry.getScore() == null ? 0L : entry.getScore().longValue());
        }

        RiskSubjectVO vo = new RiskSubjectVO();
        vo.setPrincipal(principalKey);
        vo.setPrincipalType(principalKey.startsWith(RiskPrincipal.TYPE_USER + ":")
                ? RiskPrincipal.TYPE_USER : RiskPrincipal.TYPE_IP);
        vo.setPrincipalId(principalKey.substring(principalKey.indexOf(':') + 1));
        vo.setRiskScore(score);
        RiskLevel level = levelOf(score);
        vo.setRiskLevel(level.getCode());
        vo.setRiskLevelText(level.getText());
        vo.setEventCount(entries.size());
        if (latest > 0) {
            vo.setLastEventTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(latest), ZoneId.systemDefault()));
        }

        RBucket<String> blockBucket = redissonClient.getBucket(BLOCK_PREFIX + principalKey);
        if (blockBucket.isExists()) {
            vo.setBlocked(true);
            long ttlMs = blockBucket.remainTimeToLive();
            vo.setRemainingSeconds(ttlMs > 0 ? ttlMs / 1000 : 0L);
            vo.setReason(blockBucket.get());
        } else {
            vo.setBlocked(false);
            vo.setRemainingSeconds(0L);
        }
        if (RiskPrincipal.TYPE_USER.equals(vo.getPrincipalType())) {
            Long userId = parseLong(vo.getPrincipalId());
            if (userId != null) {
                // 用户主体没有独立 IP 字段，此处留空由前端按用户维度展示
                vo.setIpaddr(null);
            }
        } else {
            vo.setIpaddr(vo.getPrincipalId());
        }
        return vo;
    }

    private Long parseLong(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public List<SecurityEventDO> getRecentEvents(String principal, int limit) {
        return securityEventService.getRecentByPrincipal(principal, limit);
    }

    // ---------------------------------------------------------------- 其它

    @Override
    public String matchBlockedUserAgent(String userAgent) {
        if (StrUtil.isBlank(userAgent)) {
            return null;
        }
        String lowerUa = userAgent.toLowerCase();
        for (String sig : BLOCKED_USER_AGENTS) {
            if (lowerUa.contains(sig)) {
                return sig;
            }
        }
        return null;
    }

    @Override
    public boolean isChallengeRequired(RiskPrincipal subject) {
        if (!systemConfigHelper.isRiskChallengeEnabled() || subject == null) {
            return false;
        }
        if (isBlocked(subject)) {
            return false;
        }
        try {
            // 已通过一次验证的主体给一个短豁免期，避免每个写请求都弹验证
            boolean passed = redissonClient.getBucket(CHALLENGE_PASS_PREFIX + subject.key()).isExists();
            if (passed) {
                return false;
            }
        } catch (Exception ignored) {
            return false;
        }
        RiskLevel level = levelOf(scoreOf(subject.key()));
        return level.getCode() >= RiskLevel.CHALLENGE.getCode();
    }

    /**
     * 标记主体已通过一次风控挑战，短时间内不再重复要求。
     */
    @Override
    public void markChallengePassed(RiskPrincipal subject) {
        if (subject == null) {
            return;
        }
        try {
            redissonClient.getBucket(CHALLENGE_PASS_PREFIX + subject.key())
                    .set("1", Duration.ofMinutes(10));
        } catch (Exception e) {
            log.debug("写入挑战豁免失败: principal={}, error={}", subject.key(), e.getMessage());
        }
    }

    private void persistEvent(RiskPrincipal subject, int delta, String eventType, String action,
                              String uri, String method, String userAgent, String reason, int total) {
        try {
            SecurityEventDO event = new SecurityEventDO();
            event.setEventType(eventType);
            event.setPrincipal(subject.key());
            event.setIpaddr(StrUtil.blankToDefault(subject.ip(), subject.key()));
            event.setRequestUri(StrUtil.nullToEmpty(uri));
            event.setRequestMethod(StrUtil.nullToEmpty(method));
            event.setUserAgent(StrUtil.sub(userAgent, 0, 500));
            event.setRiskDelta(delta);
            event.setTotalRiskScore(total);
            event.setAction(action);
            event.setReason(reason);
            event.setCreateTime(LocalDateTime.now());
            securityEventService.recordAsync(event);
        } catch (Exception e) {
            log.error("记录安全事件失败: principal={}, type={}", subject.key(), eventType, e);
        }
    }
}

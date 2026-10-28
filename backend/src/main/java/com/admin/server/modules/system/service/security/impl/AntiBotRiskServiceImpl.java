package com.admin.server.modules.system.service.security.impl;

import cn.dev33.satoken.stp.StpUtil;
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
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.KeysScanOptions;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import com.admin.server.modules.system.dal.dataobject.user.UserDO;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 行为风控与反爬防御核心服务实现。
 *
 * <p>风险分使用 Redis Hash 做「分桶计数」的滑动窗口：每个主体一个 Hash，
 * 字段 {@code s<桶号>} 记录该时间桶内的分值之和，{@code c<桶号>} 记录事件数，{@code last} 记录最近事件时间。
 * 桶宽 = 窗口 / 6，限定在 1～10 分钟之间，读取时只汇总窗口内的桶，过期桶在读写时顺带清除，
 * 因此分数会随攻击行为停止而自然回落，不需要额外的衰减任务。</p>
 *
 * <p>为什么不再用「每个事件一个成员」的有序集合：高频攻击下窗口内会堆积上万成员，
 * 每次记分与查询都要把全部成员传回应用端累加，攻击量越大单请求成本越高。
 * 分桶后单主体 Hash 最多约 2×(窗口/桶宽) 个字段（窗口 1440 分钟时 ≤ 290），成本与攻击量无关，
 * 求和在 Redis 端的 Lua 脚本内完成，一次往返。</p>
 *
 * <p>精度说明：窗口按桶对齐，实际统计范围为「最近 窗口-桶宽 ～ 窗口」，宁可略微少算也不多算，避免误封。</p>
 */
@Slf4j
@Service
public class AntiBotRiskServiceImpl implements AntiBotRiskService {

    /** 风险分窗口前缀（Hash，分桶计数）。与旧版有序集合前缀 security:risk:z: 区分，避免类型冲突 */
    private static final String RISK_WINDOW_PREFIX = "security:risk:h:";

    /** 告警去重键前缀：同一主体 + 同一事件类型 + 同一等级在去重时间内只推送一次 */
    private static final String ALERT_DEDUP_PREFIX = "security:risk:cd:alert:";

    /** 告警去重时间（秒） */
    private static final int ALERT_DEDUP_SECONDS = 300;

    /** 分桶最小宽度（毫秒） */
    private static final long MIN_BUCKET_MILLIS = 60_000L;

    /** 分桶最大宽度（毫秒） */
    private static final long MAX_BUCKET_MILLIS = 600_000L;

    /** 窗口被切分的桶数 */
    private static final int BUCKETS_PER_WINDOW = 6;

    /**
     * 记分与汇总脚本（原子执行）。
     *
     * <p>KEYS[1] 主体 Hash；ARGV：当前桶号、窗口内最小桶号、增量、Hash 过期毫秒、当前毫秒时间戳。
     * 返回 {窗口内总分, 窗口内事件数, 最近事件时间}。</p>
     */
    private static final String WINDOW_SCRIPT = """
            local delta = tonumber(ARGV[3])
            if delta > 0 then
              redis.call('HINCRBY', KEYS[1], 's' .. ARGV[1], delta)
              redis.call('HINCRBY', KEYS[1], 'c' .. ARGV[1], 1)
              redis.call('HSET', KEYS[1], 'last', ARGV[5])
              redis.call('PEXPIRE', KEYS[1], ARGV[4])
            end
            local minBucket = tonumber(ARGV[2])
            local all = redis.call('HGETALL', KEYS[1])
            local sum = 0
            local cnt = 0
            for i = 1, #all, 2 do
              local field = all[i]
              local kind = string.sub(field, 1, 1)
              if kind == 's' or kind == 'c' then
                local bucket = tonumber(string.sub(field, 2))
                if bucket == nil or bucket < minBucket then
                  redis.call('HDEL', KEYS[1], field)
                elseif kind == 's' then
                  sum = sum + tonumber(all[i + 1])
                else
                  cnt = cnt + tonumber(all[i + 1])
                end
              end
            end
            if cnt == 0 then
              redis.call('DEL', KEYS[1])
              return {0, 0, 0}
            end
            local last = tonumber(redis.call('HGET', KEYS[1], 'last') or '0')
            return {sum, cnt, last}
            """;

    /** 窗口统计结果 */
    private record WindowStat(int score, int count, long lastTime) {
        static final WindowStat EMPTY = new WindowStat(0, 0, 0L);
    }

    /** 封禁标记前缀，value 为封禁原因 */
    private static final String BLOCK_PREFIX = "security:risk:block:";

    /** 挑战通过后的豁免前缀 */
    private static final String CHALLENGE_PASS_PREFIX = "security:risk:pass:";

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

    @Resource
    private UserMapper userMapper;

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
        int total = touchScore(subject, delta, windowMinutes).score();

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
                warnScore, challengeScore, blockScore, banScore);

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
        // 告警必须去重：签名失败、重放等事件每次都会触达这里，攻击者每秒几十次请求
        // 不能变成每秒几十条 webhook。同一主体 + 事件类型 + 等级在去重期内只推一次，等级升级仍会再推。
        if (needAlert && acquireAlertSlot(eventType, subject.key(), level)) {
            riskAlertNotifier.notify(eventType, subject.key(), level.getText(), total,
                    effectiveAction, reason, uri, StrUtil.blankToDefault(subject.ip(), ""));
        }
        return total;
    }

    /**
     * 累加一次风险事件，返回窗口内的统计结果；Redis 异常时返回空统计（fail-open）。
     */
    private WindowStat touchScore(RiskPrincipal subject, int delta, int windowMinutes) {
        try {
            return evalWindow(subject.key(), delta, windowMinutes);
        } catch (Exception e) {
            log.warn("风险分写入失败: principal={}, error={}", subject.key(), e.getMessage());
            return WindowStat.EMPTY;
        }
    }

    /**
     * 在 Redis 端原子完成「累加 + 清理过期桶 + 汇总」，delta 为 0 时只读取。
     */
    private WindowStat evalWindow(String principalKey, int delta, int windowMinutes) {
        long now = System.currentTimeMillis();
        long windowMillis = windowMinutes * 60_000L;
        long bucketMillis = bucketMillis(windowMinutes);
        long currentBucket = now / bucketMillis;
        // 只统计整桶落在窗口内的部分，偏保守，避免误封
        long minBucket = (now - windowMillis) / bucketMillis + 1;
        long ttlMillis = windowMillis + 30 * 60_000L;

        List<Object> result = redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE, WINDOW_SCRIPT, RScript.ReturnType.MULTI,
                Collections.singletonList((Object) (RISK_WINDOW_PREFIX + principalKey)),
                currentBucket, minBucket, delta, ttlMillis, now);
        if (result == null || result.size() < 3) {
            return WindowStat.EMPTY;
        }
        return new WindowStat(toInt(result.get(0)), toInt(result.get(1)), toLong(result.get(2)));
    }

    /** 桶宽 = 窗口 / 6，限定在 1～10 分钟之间 */
    private long bucketMillis(int windowMinutes) {
        long raw = windowMinutes * 60_000L / BUCKETS_PER_WINDOW;
        return Math.max(MIN_BUCKET_MILLIS, Math.min(MAX_BUCKET_MILLIS, raw));
    }

    /** 兼容 Number 与可解析字符串：不同编解码器可能把 Lua 整数返回成字符串，解析失败绝不能静默吞成 0 */
    private int toInt(Object value) {
        return (int) Math.min(Integer.MAX_VALUE, toLong(value));
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof CharSequence text) {
            try {
                return Long.parseLong(text.toString().trim());
            } catch (NumberFormatException e) {
                log.warn("风险分脚本返回值无法解析: value={}", value);
            }
        } else if (value != null) {
            log.warn("风险分脚本返回了未预期的类型: type={}", value.getClass().getName());
        }
        return 0L;
    }

    /**
     * 告警去重：返回 true 表示本次允许推送。Redis 异常时不推送，宁可漏一条也不能放大成告警风暴。
     */
    private boolean acquireAlertSlot(String eventType, String principalKey, RiskLevel level) {
        try {
            return redissonClient.getBucket(ALERT_DEDUP_PREFIX + eventType + ":" + principalKey + ":" + level.getCode())
                    .setIfAbsent("1", Duration.ofSeconds(ALERT_DEDUP_SECONDS));
        } catch (Exception e) {
            log.debug("告警去重键写入失败，跳过本次推送: principal={}, error={}", principalKey, e.getMessage());
            return false;
        }
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
                systemConfigHelper.getRiskBlockScore(),
                systemConfigHelper.getRiskBanScore());
    }

    private int scoreOf(String principalKey) {
        try {
            return evalWindow(principalKey, 0, systemConfigHelper.getRiskWindowMinutes()).score();
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
            RBucket<String> bucket = redissonClient.getBucket(BLOCK_PREFIX + subject.key());
            // 只延长、不缩短：remainTimeToLive 为 -1 表示永久，-2 表示不存在。
            // 先读后写存在极小的并发窗口，最坏结果是少延长一次，不会造成误放行。
            long remainMillis = bucket.remainTimeToLive();
            if (remainMillis == -1L || remainMillis > seconds * 1000L) {
                log.info("已有更长的封禁，保持不变: principal={}, remainSeconds={}, requestSeconds={}",
                        subject.key(), remainMillis < 0 ? -1 : remainMillis / 1000, seconds);
                return;
            }
            bucket.set(StrUtil.blankToDefault(reason, "网络环境异常被系统风控封禁"),
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
                redissonClient.getKeys().delete(RISK_WINDOW_PREFIX + subject.key());
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
        int windowMinutes = systemConfigHelper.getRiskWindowMinutes();
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
            try {
                WindowStat stat = evalWindow(principalKey, 0, windowMinutes);
                if (stat.count() <= 0) {
                    continue;
                }
                RiskSubjectVO vo = buildSubjectVO(principalKey, stat);
                if (vo.getRiskScore() > 0 || Boolean.TRUE.equals(vo.getBlocked())) {
                    list.add(vo);
                }
            } catch (Exception e) {
                log.debug("读取风险主体失败: key={}, error={}", name, e.getMessage());
            }
        }

        // 批量回填登录用户的账号名与昵称
        populateUserInfos(list);

        // 关键词过滤：支持用户名、昵称、主体 Key、用户ID、IP
        if (lowerKeyword != null) {
            list = list.stream().filter(vo -> {
                if (vo.getPrincipal() != null && vo.getPrincipal().toLowerCase().contains(lowerKeyword)) {
                    return true;
                }
                if (vo.getUsername() != null && vo.getUsername().toLowerCase().contains(lowerKeyword)) {
                    return true;
                }
                if (vo.getNickname() != null && vo.getNickname().toLowerCase().contains(lowerKeyword)) {
                    return true;
                }
                if (vo.getPrincipalId() != null && vo.getPrincipalId().toLowerCase().contains(lowerKeyword)) {
                    return true;
                }
                if (vo.getIpaddr() != null && vo.getIpaddr().toLowerCase().contains(lowerKeyword)) {
                    return true;
                }
                return false;
            }).collect(Collectors.toCollection(ArrayList::new));
        }

        list.sort(Comparator.comparing(RiskSubjectVO::getRiskScore,
                Comparator.nullsLast(Comparator.<Integer>reverseOrder())));
        return list;
    }

    private void populateUserInfos(List<RiskSubjectVO> list) {
        if (list == null || list.isEmpty() || userMapper == null) {
            return;
        }
        Set<Long> userIds = list.stream()
                .filter(vo -> RiskPrincipal.TYPE_USER.equals(vo.getPrincipalType()))
                .map(RiskSubjectVO::getPrincipalId)
                .map(this::parseLong)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        try {
            List<UserDO> users = userMapper.selectByIds(userIds);
            if (users == null || users.isEmpty()) {
                return;
            }
            Map<Long, UserDO> userMap = users.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(UserDO::getId, u -> u, (a, b) -> a));
            for (RiskSubjectVO vo : list) {
                if (RiskPrincipal.TYPE_USER.equals(vo.getPrincipalType())) {
                    Long uid = parseLong(vo.getPrincipalId());
                    if (uid != null) {
                        UserDO user = userMap.get(uid);
                        if (user != null) {
                            vo.setUsername(user.getUsername());
                            vo.setNickname(user.getNickname());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("批量填充风险主体用户信息异常: error={}", e.getMessage());
        }
    }

    private RiskSubjectVO buildSubjectVO(String principalKey, WindowStat stat) {
        int score = stat.score();
        long latest = stat.lastTime();
        RiskSubjectVO vo = new RiskSubjectVO();
        vo.setPrincipal(principalKey);
        vo.setPrincipalType(principalKey.startsWith(RiskPrincipal.TYPE_USER + ":")
                ? RiskPrincipal.TYPE_USER : RiskPrincipal.TYPE_IP);
        vo.setPrincipalId(principalKey.substring(principalKey.indexOf(':') + 1));
        vo.setRiskScore(score);
        RiskLevel level = levelOf(score);
        vo.setRiskLevel(level.getCode());
        vo.setRiskLevelText(level.getText());
        vo.setEventCount(stat.count());
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
        // 总开关关闭时历史风险分不再触发挑战，与「关闭即全部失效」保持一致
        if (!systemConfigHelper.isRiskEnabled() || !systemConfigHelper.isRiskChallengeEnabled() || subject == null) {
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

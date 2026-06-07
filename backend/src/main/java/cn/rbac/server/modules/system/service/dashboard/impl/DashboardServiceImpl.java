package cn.rbac.server.modules.system.service.dashboard.impl;

import cn.hutool.json.JSONUtil;
import cn.rbac.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import cn.rbac.server.modules.system.dal.mysql.dashboard.DashboardMapper;
import cn.rbac.server.modules.system.dal.mysql.loginlog.LoginLogMapper;
import cn.rbac.server.modules.system.service.config.SystemConfigHelper;
import cn.rbac.server.modules.system.service.dashboard.DashboardService;
import cn.rbac.server.modules.system.service.dashboard.vo.DashboardStatsRow;
import cn.rbac.server.modules.system.service.file.impl.SysFileServiceImpl;
import cn.rbac.server.modules.system.service.monitor.OnlineUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final String VISIT_COUNT_KEY = "dashboard:visit:date:";
    private static final String STATS_AGGREGATE_CACHE_KEY = "dashboard:stats:aggregate";
    /** 计数类统计缓存时长（分钟级延迟可接受） */
    private static final long STATS_CACHE_MINUTES = 2;

    @Resource
    private DashboardMapper dashboardMapper;
    @Resource
    private LoginLogMapper loginLogMapper;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private OnlineUserService onlineUserService;
    @Resource
    private SystemConfigHelper systemConfigHelper;

    @Override
    public Map<String, Object> getStats(Long loginUserId) {
        if (loginUserId != null) {
            onlineUserService.touchLastAccess(loginUserId);
        }

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime yesterdayStart = todayStart.minusDays(1);

        Map<String, Object> stats = new HashMap<>();
        putConfigFields(stats);

        DashboardStatsRow row = loadAggregateStatsRow(todayStart, yesterdayStart);
        putAggregateFields(stats, row);

        stats.put("onlineCount", onlineUserService.listOnlineUsers().size());
        stats.put("todayVisits", getDayVisitCount(LocalDate.now()));
        stats.put("yesterdayVisits", getDayVisitCount(LocalDate.now().minusDays(1)));

        return stats;
    }

    @Override
    public List<LoginLogDO> getRecentLogins() {
        return loginLogMapper.selectList(new LambdaQueryWrapper<LoginLogDO>()
                .orderByDesc(LoginLogDO::getLoginTime)
                .last("LIMIT 8"));
    }

    @Override
    public void recordVisit(Long loginUserId) {
        if (loginUserId != null) {
            onlineUserService.touchLastAccess(loginUserId);
        }
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        RBucket<Long> bucket = redissonClient.getBucket(VISIT_COUNT_KEY + today);
        Long count = bucket.get();
        setWithTtl(bucket, (count == null ? 0L : count) + 1, 7, TimeUnit.DAYS);
    }

    private DashboardStatsRow loadAggregateStatsRow(LocalDateTime todayStart, LocalDateTime yesterdayStart) {
        String cacheKey = STATS_AGGREGATE_CACHE_KEY + ":" + LocalDate.now();
        RBucket<String> cacheBucket = redissonClient.getBucket(cacheKey);
        String cached = cacheBucket.get();
        if (cached != null) {
            return JSONUtil.toBean(cached, DashboardStatsRow.class);
        }
        DashboardStatsRow row = dashboardMapper.selectAggregateStats(
                todayStart,
                yesterdayStart,
                LocalDateTime.now(),
                SysFileServiceImpl.CHAT_IMAGE_PATH_PREFIX,
                SysFileServiceImpl.CHAT_FILE_PATH_PREFIX);
        setWithTtl(cacheBucket, JSONUtil.toJsonStr(row), STATS_CACHE_MINUTES, TimeUnit.MINUTES);
        return row;
    }

    private void putConfigFields(Map<String, Object> stats) {
        stats.put("platformName", systemConfigHelper.getPlatformName());
        stats.put("platformSubtitle", systemConfigHelper.getPlatformSubtitle());
        stats.put("fileMaxSizeMb", systemConfigHelper.getFileMaxSizeMb());
        stats.put("fileAllowedExtensions", systemConfigHelper.getFileAllowedExtensions());
        stats.put("tokenExpireHours", systemConfigHelper.getTokenExpireHours());
        stats.put("loginCaptchaEnabled", systemConfigHelper.isCaptchaEnabled());
        stats.put("loginCaptchaType", systemConfigHelper.getCaptchaType());
        stats.put("loginRememberMe", systemConfigHelper.isRememberMeEnabled());
        stats.put("loginMaxRetryCount", systemConfigHelper.getMaxRetryCount());
        stats.put("loginLockTimeMinutes", systemConfigHelper.getLockTimeMinutes());
        stats.put("registerEnabled", systemConfigHelper.isRegisterEnabled());
        stats.put("registerNeedAudit", systemConfigHelper.isRegisterNeedAudit());
    }

    private void putAggregateFields(Map<String, Object> stats, DashboardStatsRow row) {
        stats.put("userCount", longVal(row.getUserCount()));
        stats.put("roleCount", longVal(row.getRoleCount()));
        stats.put("menuCount", longVal(row.getMenuCount()));
        stats.put("deptCount", longVal(row.getDeptCount()));
        stats.put("postCount", longVal(row.getPostCount()));

        stats.put("userPendingCount", longVal(row.getUserPendingCount()));
        stats.put("userDisabledCount", longVal(row.getUserDisabledCount()));

        stats.put("fileCount", longVal(row.getFileCount()));

        stats.put("todayLoginSuccess", longVal(row.getTodayLoginSuccess()));
        stats.put("todayLoginFail", longVal(row.getTodayLoginFail()));
        stats.put("yesterdayLoginSuccess", longVal(row.getYesterdayLoginSuccess()));

        stats.put("ticketOpenCount", longVal(row.getTicketOpenCount()));
        stats.put("ticketOverdueCount", longVal(row.getTicketOverdueCount()));
        stats.put("approvalPendingCount", longVal(row.getApprovalPendingCount()));

        stats.put("userTrend", trendPercent(longVal(row.getUserToday()), longVal(row.getUserYesterday())));
        stats.put("roleTrend", trendPercent(longVal(row.getRoleToday()), longVal(row.getRoleYesterday())));
        stats.put("deptTrend", trendPercent(longVal(row.getDeptToday()), longVal(row.getDeptYesterday())));
        stats.put("menuTrend", 0);
    }

    private long longVal(Long value) {
        return value != null ? value : 0L;
    }

    private long getDayVisitCount(LocalDate date) {
        RBucket<Long> bucket = redissonClient.getBucket(VISIT_COUNT_KEY + date.format(DateTimeFormatter.ISO_LOCAL_DATE));
        Long count = bucket.get();
        return count != null ? count : 0;
    }

    private int trendPercent(long todayNew, long yesterdayNew) {
        if (yesterdayNew <= 0) {
            return todayNew > 0 ? 100 : 0;
        }
        return (int) Math.round((todayNew - yesterdayNew) * 100.0 / yesterdayNew);
    }

    @SuppressWarnings("deprecation")
    private <V> void setWithTtl(RBucket<V> bucket, V value, long duration, TimeUnit unit) {
        bucket.set(value, duration, unit);
    }
}

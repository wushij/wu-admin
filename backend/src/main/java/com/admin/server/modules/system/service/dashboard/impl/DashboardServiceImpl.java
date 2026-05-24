package com.admin.server.modules.system.service.dashboard.impl;

import cn.hutool.json.JSONUtil;
import com.admin.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;
import com.admin.server.modules.system.dal.mysql.dashboard.DashboardMapper;
import com.admin.server.modules.system.dal.mysql.loginlog.LoginLogMapper;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.admin.server.modules.system.service.config.SysConfigGroupService;
import com.admin.server.modules.system.service.dashboard.DashboardService;
import com.admin.server.modules.system.service.dashboard.vo.RecentLoginVO;
import com.admin.server.modules.system.service.dashboard.vo.DashboardStatsRow;
import com.admin.server.modules.infra.service.file.SysFileService;
import com.admin.server.modules.message.service.ChatService;
import com.admin.server.modules.infra.service.monitor.OnlineUserService;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final String VISIT_COUNT_KEY = "dashboard:visit:date:";
    /** v3：fileCount 改为实时查文件管理服务，与列表口径一致 */
    private static final String STATS_AGGREGATE_CACHE_KEY = "dashboard:stats:aggregate:v3";
    /** 计数类统计缓存时长（分钟级延迟可接受） */
    private static final long STATS_CACHE_MINUTES = 2;

    @Resource
    private DashboardMapper dashboardMapper;
    @Resource
    private LoginLogMapper loginLogMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private OnlineUserService onlineUserService;
    @Resource
    private SystemConfigHelper systemConfigHelper;
    @Resource
    private SysConfigGroupService configGroupService;
    @Resource
    private ChatService chatService;
    @Resource
    private SysFileService sysFileService;

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
        putLoginStatsFresh(stats, todayStart, yesterdayStart);
        putFileCountFresh(stats);
        putJobStatsFresh(stats);
        putApprovalPendingFresh(stats, loginUserId);
        putTicketPendingFresh(stats, loginUserId);

        stats.put("onlineCount", onlineUserService.countOnlineUsers());
        stats.put("todayVisits", getDayVisitCount(LocalDate.now()));
        stats.put("yesterdayVisits", getDayVisitCount(LocalDate.now().minusDays(1)));
        stats.put("configGroupCount", configGroupService.listAll().size());
        if (loginUserId != null) {
            stats.put("chatUnreadCount", chatService.unreadCount(loginUserId));
        }

        return stats;
    }

    @Override
    public List<RecentLoginVO> getRecentLogins() {
        List<LoginLogDO> logs = loginLogMapper.selectList(new LambdaQueryWrapper<LoginLogDO>()
                .orderByDesc(LoginLogDO::getLoginTime)
                .last("LIMIT 8"));
        Set<Long> userIds = logs.stream()
                .map(LoginLogDO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UserDO> userMap = userIds.isEmpty()
                ? Map.of()
                : userMapper.selectByIds(userIds).stream()
                        .collect(Collectors.toMap(UserDO::getId, u -> u, (a, b) -> a));
        return logs.stream().map(log -> {
            RecentLoginVO vo = new RecentLoginVO();
            vo.setUserId(log.getUserId());
            vo.setUsername(log.getUsername());
            vo.setIpaddr(log.getIpaddr());
            vo.setLoginLocation(log.getLoginLocation());
            vo.setBrowser(log.getBrowser());
            vo.setOs(log.getOs());
            vo.setStatus(log.getStatus());
            vo.setLoginTime(log.getLoginTime());
            UserDO user = log.getUserId() != null ? userMap.get(log.getUserId()) : null;
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }
            return vo;
        }).collect(Collectors.toList());
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
                LocalDateTime.now());
        setWithTtl(cacheBucket, JSONUtil.toJsonStr(row), STATS_CACHE_MINUTES, TimeUnit.MINUTES);
        return row;
    }

    private void putConfigFields(Map<String, Object> stats) {
        systemConfigHelper.fillDashboardConfigFields(stats);
    }

    private void putAggregateFields(Map<String, Object> stats, DashboardStatsRow row) {
        stats.put("userCount", longVal(row.getUserCount()));
        stats.put("roleCount", longVal(row.getRoleCount()));
        stats.put("menuCount", longVal(row.getMenuCount()));
        stats.put("deptCount", longVal(row.getDeptCount()));
        stats.put("postCount", longVal(row.getPostCount()));

        stats.put("userPendingCount", longVal(row.getUserPendingCount()));
        stats.put("userDisabledCount", longVal(row.getUserDisabledCount()));

        stats.put("ticketOpenCount", longVal(row.getTicketOpenCount()));
        stats.put("ticketOverdueCount", longVal(row.getTicketOverdueCount()));
        stats.put("approvalPendingCount", longVal(row.getApprovalPendingCount()));

        stats.put("userTrend", trendPercent(longVal(row.getUserToday()), longVal(row.getUserYesterday())));
        stats.put("roleTrend", trendPercent(longVal(row.getRoleToday()), longVal(row.getRoleYesterday())));
        stats.put("deptTrend", trendPercent(longVal(row.getDeptToday()), longVal(row.getDeptYesterday())));
        stats.put("menuTrend", 0);
    }

    /**
     * 今日登录成功/失败与欢迎区「在线用户」「今日访问」一样实时查询。
     * 聚合缓存 2 分钟，登录后进工作台会看到旧数字。
     */
    private void putLoginStatsFresh(Map<String, Object> stats, LocalDateTime todayStart, LocalDateTime yesterdayStart) {
        stats.put("todayLoginSuccess", loginLogMapper.selectCount(new LambdaQueryWrapper<LoginLogDO>()
                .eq(LoginLogDO::getStatus, 0)
                .ge(LoginLogDO::getLoginTime, todayStart)));
        stats.put("todayLoginFail", loginLogMapper.selectCount(new LambdaQueryWrapper<LoginLogDO>()
                .eq(LoginLogDO::getStatus, 1)
                .ge(LoginLogDO::getLoginTime, todayStart)));
        stats.put("yesterdayLoginSuccess", loginLogMapper.selectCount(new LambdaQueryWrapper<LoginLogDO>()
                .eq(LoginLogDO::getStatus, 0)
                .ge(LoginLogDO::getLoginTime, yesterdayStart)
                .lt(LoginLogDO::getLoginTime, todayStart)));
    }

    /** 文件数量每次实时查询，与文件管理列表口径一致（排除聊天目录、仅未删除） */
    private void putFileCountFresh(Map<String, Object> stats) {
        stats.put("fileCount", sysFileService.countForGroupSidebar(null, null, null));
    }

    /** 定时任务数量每次实时查询，不走聚合缓存 */
    private void putJobStatsFresh(Map<String, Object> stats) {
        long jobTotal = dashboardMapper.countJobTotal();
        long jobRunning = dashboardMapper.countJobRunning();
        stats.put("jobTotalCount", jobTotal);
        stats.put("jobRunningCount", jobRunning);
        stats.put("jobPausedCount", Math.max(0, jobTotal - jobRunning));
    }

    /** 待我审批数量：仅统计指定当前用户为审批人且未处理的单据 */
    private void putApprovalPendingFresh(Map<String, Object> stats, Long loginUserId) {
        long count = loginUserId != null && loginUserId > 0
                ? dashboardMapper.countApprovalPendingByApprover(loginUserId)
                : 0L;
        stats.put("approvalPendingCount", count);
    }

    /** 待我处理工单：指定当前用户为处理人，或全员通知（assignee=0）且状态为待处理 */
    private void putTicketPendingFresh(Map<String, Object> stats, Long loginUserId) {
        if (loginUserId == null || loginUserId <= 0) {
            stats.put("ticketOpenCount", 0L);
            stats.put("ticketOverdueCount", 0L);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        stats.put("ticketOpenCount", dashboardMapper.countTicketOpenByAssignee(loginUserId));
        stats.put("ticketOverdueCount", dashboardMapper.countTicketOverdueByAssignee(loginUserId, now));
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
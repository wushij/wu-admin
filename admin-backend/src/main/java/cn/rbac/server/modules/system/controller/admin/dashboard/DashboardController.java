package cn.rbac.server.modules.system.controller.admin.dashboard;

import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.modules.system.dal.dataobject.approval.ApprovalFormDO;
import cn.rbac.server.modules.system.dal.dataobject.dept.DeptDO;
import cn.rbac.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import cn.rbac.server.modules.system.dal.dataobject.permission.MenuDO;
import cn.rbac.server.modules.system.dal.dataobject.permission.RoleDO;
import cn.rbac.server.modules.system.dal.dataobject.ticket.TicketDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.dal.mysql.dept.DeptMapper;
import cn.rbac.server.modules.system.dal.mysql.file.SysFileMapper;
import cn.rbac.server.modules.system.dal.mysql.loginlog.LoginLogMapper;
import cn.rbac.server.modules.system.dal.mysql.permission.MenuMapper;
import cn.rbac.server.modules.system.dal.mysql.permission.RoleMapper;
import cn.rbac.server.modules.system.dal.mysql.post.PostMapper;
import cn.rbac.server.modules.system.dal.mysql.approval.ApprovalFormMapper;
import cn.rbac.server.modules.system.dal.mysql.ticket.TicketMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.service.config.SystemConfigHelper;
import cn.rbac.server.modules.system.service.monitor.OnlineUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Tag(name = "工作台统计")
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Resource
    private UserMapper userMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private MenuMapper menuMapper;
    @Resource
    private DeptMapper deptMapper;
    @Resource
    private PostMapper postMapper;
    @Resource
    private TicketMapper ticketMapper;
    @Resource
    private LoginLogMapper loginLogMapper;
    @Resource
    private SysFileMapper sysFileMapper;
    @Resource
    private ApprovalFormMapper approvalFormMapper;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private OnlineUserService onlineUserService;
    @Resource
    private SystemConfigHelper systemConfigHelper;

    private static final String VISIT_COUNT_KEY = "dashboard:visit:date:";

    @Operation(summary = "工作台统计数据")
    @GetMapping("/stats")
    public CommonResult<Map<String, Object>> getStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime yesterdayStart = todayStart.minusDays(1);

        Map<String, Object> stats = new HashMap<>();
        stats.put("platformName", systemConfigHelper.getPlatformName());
        stats.put("platformSubtitle", systemConfigHelper.getPlatformSubtitle());

        stats.put("userCount", userMapper.selectCount(userWrapper()));
        stats.put("roleCount", roleMapper.selectCount(roleWrapper()));
        stats.put("menuCount", menuMapper.selectCount(menuWrapper()));
        stats.put("deptCount", deptMapper.selectCount(deptWrapper()));
        stats.put("postCount", postMapper.selectCount(postWrapper()));

        stats.put("userPendingCount", userMapper.selectCount(userWrapper().eq(UserDO::getStatus, 2)));
        stats.put("userDisabledCount", userMapper.selectCount(userWrapper().eq(UserDO::getStatus, 0)));

        stats.put("fileCount", sysFileMapper.selectCount(null));
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

        stats.put("todayLoginSuccess", countLoginLog(todayStart, null, 0));
        stats.put("todayLoginFail", countLoginLog(todayStart, null, 1));
        stats.put("yesterdayLoginSuccess", countLoginLog(yesterdayStart, todayStart, 0));

        stats.put("ticketOpenCount", ticketMapper.selectCount(new LambdaQueryWrapper<TicketDO>()
                .eq(TicketDO::getStatus, "OPEN")));
        stats.put("ticketOverdueCount", ticketMapper.selectCount(new LambdaQueryWrapper<TicketDO>()
                .in(TicketDO::getStatus, "OPEN", "IN_PROGRESS")
                .isNotNull(TicketDO::getDeadline)
                .lt(TicketDO::getDeadline, LocalDateTime.now())));
        stats.put("approvalPendingCount", approvalFormMapper.selectCount(new LambdaQueryWrapper<ApprovalFormDO>()
                .eq(ApprovalFormDO::getStatus, "SUBMITTED")));

        stats.put("onlineCount", onlineUserService.listOnlineUsers().size());
        stats.put("todayVisits", getDayVisitCount(LocalDate.now()));
        stats.put("yesterdayVisits", getDayVisitCount(LocalDate.now().minusDays(1)));

        long userToday = countUserCreatedSince(todayStart);
        long userYesterday = countUserCreatedBetween(yesterdayStart, todayStart);
        long roleToday = countRoleCreatedSince(todayStart);
        long roleYesterday = countRoleCreatedBetween(yesterdayStart, todayStart);
        long deptToday = countDeptCreatedSince(todayStart);
        long deptYesterday = countDeptCreatedBetween(yesterdayStart, todayStart);

        stats.put("userTrend", trendPercent(userToday, userYesterday));
        stats.put("roleTrend", trendPercent(roleToday, roleYesterday));
        stats.put("deptTrend", trendPercent(deptToday, deptYesterday));
        stats.put("menuTrend", 0);

        return CommonResult.success(stats);
    }

    @Operation(summary = "最近登录记录")
    @GetMapping("/recent-logins")
    public CommonResult<List<LoginLogDO>> recentLogins() {
        List<LoginLogDO> list = loginLogMapper.selectList(new LambdaQueryWrapper<LoginLogDO>()
                .orderByDesc(LoginLogDO::getLoginTime)
                .last("LIMIT 8"));
        return CommonResult.success(list);
    }

    @Operation(summary = "记录工作台访问")
    @GetMapping("/visit")
    public CommonResult<Void> recordVisit() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        RBucket<Long> bucket = redissonClient.getBucket(VISIT_COUNT_KEY + today);
        Long count = bucket.get();
        bucket.set((count == null ? 0L : count) + 1, 7, TimeUnit.DAYS);
        return CommonResult.success(null);
    }

    private LambdaQueryWrapper<UserDO> userWrapper() {
        return new LambdaQueryWrapper<UserDO>().eq(UserDO::getDeleted, 0);
    }

    private LambdaQueryWrapper<RoleDO> roleWrapper() {
        return new LambdaQueryWrapper<RoleDO>().eq(RoleDO::getDeleted, 0);
    }

    private LambdaQueryWrapper<MenuDO> menuWrapper() {
        return new LambdaQueryWrapper<MenuDO>().eq(MenuDO::getDeleted, 0);
    }

    private LambdaQueryWrapper<DeptDO> deptWrapper() {
        return new LambdaQueryWrapper<DeptDO>().eq(DeptDO::getDeleted, 0);
    }

    private LambdaQueryWrapper<cn.rbac.server.modules.system.dal.dataobject.post.PostDO> postWrapper() {
        return new LambdaQueryWrapper<cn.rbac.server.modules.system.dal.dataobject.post.PostDO>()
                .eq(cn.rbac.server.modules.system.dal.dataobject.post.PostDO::getDeleted, 0);
    }

    private long countLoginLog(LocalDateTime from, LocalDateTime to, int status) {
        LambdaQueryWrapper<LoginLogDO> w = new LambdaQueryWrapper<LoginLogDO>()
                .eq(LoginLogDO::getStatus, status)
                .ge(LoginLogDO::getLoginTime, from);
        if (to != null) {
            w.lt(LoginLogDO::getLoginTime, to);
        }
        return loginLogMapper.selectCount(w);
    }

    private long countUserCreatedSince(LocalDateTime since) {
        return userMapper.selectCount(userWrapper().ge(UserDO::getCreateTime, since));
    }

    private long countUserCreatedBetween(LocalDateTime from, LocalDateTime to) {
        return userMapper.selectCount(userWrapper().ge(UserDO::getCreateTime, from).lt(UserDO::getCreateTime, to));
    }

    private long countRoleCreatedSince(LocalDateTime since) {
        return roleMapper.selectCount(roleWrapper().ge(RoleDO::getCreateTime, since));
    }

    private long countRoleCreatedBetween(LocalDateTime from, LocalDateTime to) {
        return roleMapper.selectCount(roleWrapper().ge(RoleDO::getCreateTime, from).lt(RoleDO::getCreateTime, to));
    }

    private long countDeptCreatedSince(LocalDateTime since) {
        return deptMapper.selectCount(deptWrapper().ge(DeptDO::getCreateTime, since));
    }

    private long countDeptCreatedBetween(LocalDateTime from, LocalDateTime to) {
        return deptMapper.selectCount(deptWrapper().ge(DeptDO::getCreateTime, from).lt(DeptDO::getCreateTime, to));
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
}

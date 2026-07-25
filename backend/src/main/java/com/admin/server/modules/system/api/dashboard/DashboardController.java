package com.admin.server.modules.system.api.dashboard;

import com.admin.server.common.pojo.CommonResult;
import com.admin.server.modules.system.service.dashboard.vo.RecentLoginVO;
import com.admin.server.modules.system.service.dashboard.DashboardService;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

@Tag(name = "工作台统计")
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    @Operation(summary = "工作台统计数据")
    @GetMapping("/stats")
    @PreAuthorize("@ss.hasRead('dashboard:stats:view')")
    public CommonResult<Map<String, Object>> getStats() {
        Long loginUserId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        return CommonResult.success(dashboardService.getStats(loginUserId));
    }

    @Operation(summary = "最近登录记录")
    @GetMapping("/recent-logins")
    @PreAuthorize("@ss.hasRead('dashboard:stats:view')")
    public CommonResult<List<RecentLoginVO>> recentLogins() {
        return CommonResult.success(dashboardService.getRecentLogins());
    }

    @Operation(summary = "记录工作台访问")
    @GetMapping("/visit")
    @PreAuthorize("@ss.hasRead('dashboard:stats:view')")
    public CommonResult<Void> recordVisit() {
        Long loginUserId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        dashboardService.recordVisit(loginUserId);
        return CommonResult.success(null);
    }
}

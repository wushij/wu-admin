package com.admin.server.modules.system.api.security;

import com.admin.server.common.core.CommonResult;
import com.admin.server.common.core.PageResult;
import com.admin.server.framework.log.annotation.Log;
import com.admin.server.framework.security.api.RequirePermission;
import com.admin.server.modules.system.api.security.vo.RiskSubjectVO;
import com.admin.server.modules.system.dal.dataobject.security.SecurityEventDO;
import com.admin.server.modules.system.service.security.AntiBotRiskService;
import com.admin.server.modules.system.service.security.RiskSubjectPage;
import com.admin.server.modules.system.service.security.SecurityEventService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 安全审计与行为风控 Controller。
 */
@Tag(name = "安全审计与行为风控")
@RestController
@RequestMapping("/system/security")
public class SecurityRiskController {

    @Resource
    private SecurityEventService securityEventService;

    @Resource
    private AntiBotRiskService antiBotRiskService;

    @Operation(summary = "获取安全拦截事件列表")
    @GetMapping("/events")
    @RequirePermission("system:security:query")
    public CommonResult<PageResult<SecurityEventDO>> listEvents(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String ipaddr,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String action) {
        Page<SecurityEventDO> page = securityEventService.getPage(pageNo, pageSize, ipaddr, eventType, action);
        return CommonResult.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @Operation(summary = "获取安全拦截大盘统计数据")
    @GetMapping("/stats")
    @RequirePermission("system:security:query")
    public CommonResult<Map<String, Object>> getStats() {
        return CommonResult.success(securityEventService.getDashboardStats());
    }

    @Operation(summary = "获取当前处于封禁中的风险名单")
    @GetMapping("/risk-list")
    @RequirePermission("system:security:query")
    public CommonResult<List<RiskSubjectVO>> getRiskList() {
        return CommonResult.success(antiBotRiskService.getActiveRiskList());
    }

    @Operation(summary = "风险主体分页列表（含未封禁的高风险主体）")
    @GetMapping("/risk-subjects")
    @RequirePermission("system:security:query")
    public CommonResult<PageResult<RiskSubjectVO>> listRiskSubjects(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) Boolean blocked) {
        RiskSubjectPage page = antiBotRiskService.getRiskSubjectPage(pageNo, pageSize, keyword, level, blocked);
        return CommonResult.success(PageResult.of(page.records(), page.total()));
    }

    @Operation(summary = "查询风险主体最近的安全事件")
    @GetMapping("/risk-subject/events")
    @RequirePermission("system:security:query")
    public CommonResult<List<SecurityEventDO>> recentEvents(
            @RequestParam String principal,
            @RequestParam(defaultValue = "20") Integer limit) {
        return CommonResult.success(antiBotRiskService.getRecentEvents(principal, limit));
    }

    @Operation(summary = "获取风险主体当前分数与等级")
    @GetMapping("/risk-subject/score")
    @RequirePermission("system:security:query")
    public CommonResult<Map<String, Object>> riskScore(@RequestParam String principal) {
        int score = antiBotRiskService.getRiskScore(principal);
        var level = antiBotRiskService.getRiskLevel(principal);
        return CommonResult.success(Map.of(
                "principal", principal,
                "score", score,
                "level", level.getCode(),
                "levelText", level.getText()
        ));
    }

    @Log(title = "安全风控", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "一键解除主体封禁")
    @PostMapping("/unblock")
    @RequirePermission("system:security:unblock")
    public CommonResult<Boolean> unblock(@RequestParam String principal,
                                         @RequestParam(defaultValue = "true") Boolean resetRisk) {
        antiBotRiskService.unblockPrincipal(principal, Boolean.TRUE.equals(resetRisk));
        return CommonResult.success(true);
    }

    @Log(title = "安全风控", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "手动封禁主体")
    @PostMapping("/block")
    @RequirePermission("system:security:block")
    public CommonResult<Boolean> block(@RequestParam String principal,
                                       @RequestParam(defaultValue = "3600") Long durationSeconds,
                                       @RequestParam(defaultValue = "管理员手动封禁") String reason) {
        antiBotRiskService.blockPrincipal(principal, durationSeconds, reason);
        return CommonResult.success(true);
    }

    @Log(title = "安全风控", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除安全事件")
    @DeleteMapping("/events/{id}")
    @RequirePermission("system:security:delete")
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        securityEventService.delete(id);
        return CommonResult.success(true);
    }

    @Log(title = "安全风控", businessType = Log.BusinessType.DELETE, isSaveRequestData = false)
    @Operation(summary = "清空安全事件日志")
    @DeleteMapping("/events/clear")
    @RequirePermission("system:security:clear")
    public CommonResult<Boolean> clear() {
        securityEventService.clear();
        return CommonResult.success(true);
    }
}

package cn.rbac.server.modules.system.controller.admin.monitor;

import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.dal.dataobject.monitor.ApiAccessLogDO;
import cn.rbac.server.modules.system.service.monitor.ApiAccessLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Tag(name = "API 访问统计")
@RestController
@RequestMapping("/monitor/api-access")
public class ApiAccessLogController {

    @Resource
    private ApiAccessLogService apiAccessLogService;

    @Operation(summary = "分页查询 API 访问日志")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('monitor:apiAccess:query')")
    public CommonResult<PageResult<ApiAccessLogDO>> page(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String apiPath,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) Integer success,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return CommonResult.success(
                apiAccessLogService.page(pageNo, pageSize, userId, apiPath, method, success, startTime, endTime));
    }

    @Operation(summary = "获取统计数据")
    @GetMapping("/statistics")
    @PreAuthorize("@ss.hasPermission('monitor:apiAccess:query')")
    public CommonResult<Map<String, Object>> statistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(6);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        return CommonResult.success(apiAccessLogService.getStatistics(startDate, endDate));
    }
}

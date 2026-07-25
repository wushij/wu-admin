package com.admin.server.modules.system.api.operlog;

import com.admin.server.common.pojo.CommonResult;
import com.admin.server.common.pojo.PageResult;
import com.admin.server.framework.log.annotation.Log;
import com.admin.server.modules.system.dal.dataobject.operlog.OperLogDO;
import com.admin.server.modules.system.service.operlog.OperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@Tag(name = "操作日志")
@RestController
@RequestMapping("/system/oper-log")
public class OperLogController {

    @Resource
    private OperLogService operLogService;

    @GetMapping("/page")
    @Operation(summary = "操作日志分页")
    @PreAuthorize("@ss.hasPermission('system:operLog:query')")
    public CommonResult<PageResult<OperLogDO>> page(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String operName,
            @RequestParam(required = false) Integer status) {
        return CommonResult.success(operLogService.page(pageNo, pageSize, title, operName, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除操作日志")
    @PreAuthorize("@ss.hasPermission('system:operLog:delete')")
    @Log(title = "操作日志", businessType = Log.BusinessType.DELETE)
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        operLogService.delete(id);
        return CommonResult.success(true);
    }

    @DeleteMapping("/clean")
    @Operation(summary = "清空操作日志")
    @PreAuthorize("@ss.hasPermission('system:operLog:clear')")
    @Log(title = "操作日志", businessType = Log.BusinessType.DELETE)
    public CommonResult<Boolean> clean() {
        operLogService.clean();
        return CommonResult.success(true);
    }
}

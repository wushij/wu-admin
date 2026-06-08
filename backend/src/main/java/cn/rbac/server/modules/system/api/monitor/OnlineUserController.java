package cn.rbac.server.modules.system.api.monitor;

import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.framework.export.ExportFormat;
import cn.rbac.server.framework.export.ExportScope;
import cn.rbac.server.framework.log.annotation.Log;
import cn.rbac.server.modules.system.api.monitor.vo.OnlineUserVO;
import cn.rbac.server.modules.system.service.export.ListExportService;
import cn.rbac.server.modules.system.service.monitor.OnlineUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Tag(name = "在线用户")
@RestController
@RequestMapping("/monitor/online")
public class OnlineUserController {

    @Resource
    private OnlineUserService onlineUserService;
    @Resource
    private ListExportService listExportService;

    @Operation(summary = "在线用户列表")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('monitor:online:list')")
    public CommonResult<List<OnlineUserVO>> list() {
        return CommonResult.success(onlineUserService.listOnlineUsers());
    }

    @Log(title = "在线用户", businessType = Log.BusinessType.EXPORT, isSaveResponseData = false)
    @Operation(summary = "导出在线用户")
    @GetMapping("/export")
    @PreAuthorize("@ss.hasPermission('monitor:online:list')")
    public void export(HttpServletResponse response,
                       @RequestParam(defaultValue = "xlsx") String format,
                       @RequestParam(defaultValue = "filtered") String scope,
                       @RequestParam(required = false) Integer pageNo,
                       @RequestParam(required = false) Integer pageSize) throws IOException {
        listExportService.exportOnlineUsers(response, ExportFormat.fromParam(format),
                ExportScope.fromParam(scope), pageNo, pageSize);
    }

    @Operation(summary = "强退用户")
    @DeleteMapping("/{userId}")
    @PreAuthorize("@ss.hasPermission('monitor:online:forceLogout')")
    public CommonResult<Boolean> forceLogout(@PathVariable Long userId) {
        onlineUserService.forceLogout(userId);
        return CommonResult.success(true);
    }
}

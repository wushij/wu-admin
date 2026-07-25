package com.admin.server.modules.system.api.export;

import com.admin.server.framework.export.ExportFormat;
import com.admin.server.framework.export.ExportScope;
import com.admin.server.framework.log.annotation.Log;
import com.admin.server.modules.system.service.export.ListExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Tag(name = "数据导出")
@RestController
@RequestMapping("/system/export")
public class DataExportController {

    @Resource
    private ListExportService listExportService;

    @Log(title = "用户管理", businessType = Log.BusinessType.EXPORT, isSaveResponseData = false)
    @Operation(summary = "导出用户列表")
    @GetMapping("/user")
    @PreAuthorize("@ss.hasRead('system:user:list')")
    public void exportUser(HttpServletResponse response,
                           @RequestParam(defaultValue = "xlsx") String format,
                           @RequestParam(defaultValue = "filtered") String scope,
                           @RequestParam(required = false) Integer pageNo,
                           @RequestParam(required = false) Integer pageSize,
                           @RequestParam(required = false) String username,
                           @RequestParam(required = false) String mobile,
                           @RequestParam(required = false) Integer status,
                           @RequestParam(required = false) Long deptId,
                           @RequestParam(required = false) Long postId) throws IOException {
        listExportService.exportUsers(response, ExportFormat.fromParam(format), ExportScope.fromParam(scope),
                pageNo, pageSize, username, mobile, status, deptId, postId);
    }

    @Log(title = "登录日志", businessType = Log.BusinessType.EXPORT, isSaveResponseData = false)
    @Operation(summary = "导出登录日志")
    @GetMapping("/login-log")
    @PreAuthorize("@ss.hasPermission('system:loginLog:query')")
    public void exportLoginLog(HttpServletResponse response,
                               @RequestParam(defaultValue = "xlsx") String format,
                               @RequestParam(defaultValue = "filtered") String scope,
                               @RequestParam(required = false) Integer pageNo,
                               @RequestParam(required = false) Integer pageSize,
                               @RequestParam(required = false) String username,
                               @RequestParam(required = false) String ipaddr,
                               @RequestParam(required = false) Integer status) throws IOException {
        listExportService.exportLoginLogs(response, ExportFormat.fromParam(format), ExportScope.fromParam(scope),
                pageNo, pageSize, username, ipaddr, status);
    }

    @Log(title = "操作日志", businessType = Log.BusinessType.EXPORT, isSaveResponseData = false)
    @Operation(summary = "导出操作日志")
    @GetMapping("/oper-log")
    @PreAuthorize("@ss.hasPermission('system:operLog:query')")
    public void exportOperLog(HttpServletResponse response,
                              @RequestParam(defaultValue = "xlsx") String format,
                              @RequestParam(defaultValue = "filtered") String scope,
                              @RequestParam(required = false) Integer pageNo,
                              @RequestParam(required = false) Integer pageSize,
                              @RequestParam(required = false) String title,
                              @RequestParam(required = false) String operName,
                              @RequestParam(required = false) Integer status) throws IOException {
        listExportService.exportOperLogs(response, ExportFormat.fromParam(format), ExportScope.fromParam(scope),
                pageNo, pageSize, title, operName, status);
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.EXPORT, isSaveResponseData = false)
    @Operation(summary = "导出工单列表")
    @GetMapping("/ticket")
    @PreAuthorize("@ss.hasRead('system:ticket:list')")
    public void exportTicket(HttpServletResponse response,
                             @RequestParam(defaultValue = "xlsx") String format,
                             @RequestParam(defaultValue = "filtered") String scope,
                             @RequestParam(required = false) Integer pageNo,
                             @RequestParam(required = false) Integer pageSize,
                             @RequestParam(required = false) String title,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String priority,
                             @RequestParam(required = false) Long assigneeUserId) throws IOException {
        listExportService.exportTickets(response, ExportFormat.fromParam(format), ExportScope.fromParam(scope),
                pageNo, pageSize, title, status, priority, assigneeUserId);
    }

    @Log(title = "审批单中心", businessType = Log.BusinessType.EXPORT, isSaveResponseData = false)
    @Operation(summary = "导出审批单列表")
    @GetMapping("/approval")
    @PreAuthorize("@ss.hasRead('system:approval:list')")
    public void exportApproval(HttpServletResponse response,
                               @RequestParam(defaultValue = "xlsx") String format,
                               @RequestParam(defaultValue = "filtered") String scope,
                               @RequestParam(required = false) Integer pageNo,
                               @RequestParam(required = false) Integer pageSize,
                               @RequestParam(required = false) String title,
                               @RequestParam(required = false) String formType,
                               @RequestParam(required = false) String status) throws IOException {
        listExportService.exportApprovals(response, ExportFormat.fromParam(format), ExportScope.fromParam(scope),
                pageNo, pageSize, title, formType, status);
    }
}

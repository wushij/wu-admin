package cn.rbac.server.modules.system.api.loginlog;

import cn.rbac.server.framework.log.annotation.Log;
import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import cn.rbac.server.modules.system.service.loginlog.LoginLogService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * 登录日志 Controller
 */
@Tag(name = "登录日志")
@RestController
@RequestMapping("/system/login-log")
public class LoginLogController {

    @Resource
    private LoginLogService loginLogService;

    @Operation(summary = "获取登录日志列表")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('system:loginLog:list')")
    public CommonResult<PageResult<LoginLogDO>> list(@RequestParam(defaultValue = "1") Integer pageNo,
                                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                                     @RequestParam(required = false) String username,
                                                     @RequestParam(required = false) String ipaddr,
                                                     @RequestParam(required = false) Integer status) {
        Page<LoginLogDO> page = loginLogService.getPage(pageNo, pageSize, username, ipaddr, status);
        return CommonResult.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @Log(title = "登录日志", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除登录日志")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermission('system:loginLog:delete')")
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        loginLogService.delete(id);
        return CommonResult.success(true);
    }

    @Log(title = "登录日志", businessType = Log.BusinessType.DELETE, isSaveRequestData = false)
    @Operation(summary = "清空登录日志")
    @DeleteMapping("/clear")
    @PreAuthorize("@ss.hasPermission('system:loginLog:clear')")
    public CommonResult<Boolean> clear() {
        loginLogService.clear();
        return CommonResult.success(true);
    }
}

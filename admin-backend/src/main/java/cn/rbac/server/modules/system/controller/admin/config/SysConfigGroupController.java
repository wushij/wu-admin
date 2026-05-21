package cn.rbac.server.modules.system.controller.admin.config;

import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.framework.log.annotation.Log;
import cn.rbac.server.modules.system.dal.dataobject.config.SysConfigGroupDO;
import cn.rbac.server.modules.system.service.config.SysConfigGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Tag(name = "系统配置")
@RestController
@RequestMapping("/system/config-group")
public class SysConfigGroupController {

    @Resource
    private SysConfigGroupService configGroupService;

    @GetMapping("/list")
    @Operation(summary = "配置分组列表")
    @PreAuthorize("@ss.hasPermission('system:config:list')")
    public CommonResult<List<SysConfigGroupDO>> list() {
        return CommonResult.success(configGroupService.listAll());
    }

    @GetMapping("/{groupCode}")
    @Operation(summary = "获取配置分组")
    @PreAuthorize("@ss.hasPermission('system:config:list')")
    public CommonResult<SysConfigGroupDO> get(@PathVariable String groupCode) {
        SysConfigGroupDO row = configGroupService.getByGroupCode(groupCode);
        if (row == null) {
            return CommonResult.error(404, "配置分组不存在");
        }
        return CommonResult.success(row);
    }

    @PutMapping("/{groupCode}")
    @Operation(summary = "更新配置分组")
    @PreAuthorize("@ss.hasPermission('system:config:update')")
    @Log(title = "系统配置", businessType = Log.BusinessType.UPDATE)
    public CommonResult<Boolean> update(@PathVariable String groupCode, @RequestBody Map<String, String> body) {
        String configValue = body.get("configValue");
        if (configValue == null || configValue.isBlank()) {
            return CommonResult.error(400, "configValue 不能为空");
        }
        try {
            configGroupService.updateConfig(groupCode, configValue);
            return CommonResult.success(true);
        } catch (IllegalArgumentException e) {
            return CommonResult.error(400, e.getMessage());
        }
    }
}

package cn.rbac.server.modules.system.api.file;

import cn.rbac.server.framework.log.annotation.Log;
import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.modules.system.dal.dataobject.file.SysFileGroupDO;
import cn.rbac.server.modules.system.service.file.SysFileGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Map;

@Tag(name = "文件分组")
@RestController
@RequestMapping("/system/file-group")
public class SysFileGroupController {

    @Resource
    private SysFileGroupService fileGroupService;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasRead('sys:file:list')")
    public CommonResult<Map<String, Object>> list() {
        return CommonResult.success(fileGroupService.listWithUngroupedCount());
    }

    @Log(title = "文件分组", businessType = Log.BusinessType.INSERT)
    @PostMapping
    @PreAuthorize("@ss.hasPermission('sys:file:upload')")
    public CommonResult<Boolean> create(@RequestBody SysFileGroupDO group) {
        fileGroupService.create(group);
        return CommonResult.success(true);
    }

    @Log(title = "文件分组", businessType = Log.BusinessType.UPDATE)
    @PutMapping
    @PreAuthorize("@ss.hasPermission('sys:file:upload')")
    public CommonResult<Boolean> update(@RequestBody SysFileGroupDO group) {
        fileGroupService.update(group);
        return CommonResult.success(true);
    }

    @Log(title = "文件分组", businessType = Log.BusinessType.DELETE)
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermission('sys:file:delete')")
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        fileGroupService.delete(id);
        return CommonResult.success(true);
    }
}

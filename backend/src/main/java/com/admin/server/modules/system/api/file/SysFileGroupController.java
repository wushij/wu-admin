package com.admin.server.modules.system.api.file;

import com.admin.server.framework.log.annotation.Log;
import com.admin.server.common.pojo.CommonResult;
import com.admin.server.modules.system.dal.dataobject.file.SysFileGroupDO;
import com.admin.server.modules.system.service.file.SysFileGroupService;
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
    public CommonResult<Map<String, Object>> list(
            @RequestParam(required = false) String fileCategory) {
        return CommonResult.success(fileGroupService.listWithUngroupedCount(fileCategory));
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

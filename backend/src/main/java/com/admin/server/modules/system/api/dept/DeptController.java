package com.admin.server.modules.system.api.dept;

import com.admin.server.common.pojo.CommonResult;
import com.admin.server.common.pojo.PageParam;
import com.admin.server.common.pojo.PageResult;
import com.admin.server.framework.log.annotation.Log;
import com.admin.server.modules.system.dal.dataobject.dept.DeptDO;
import com.admin.server.modules.system.service.dept.DeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "部门管理")
@RestController
@RequestMapping("/system/dept")
public class DeptController {

    @Resource
    private DeptService deptService;

    @GetMapping("/tree")
    @Operation(summary = "部门树")
    @PreAuthorize("@ss.hasRead('system:dept:list')")
    public CommonResult<List<DeptDO>> tree(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        return CommonResult.success(deptService.tree(name, status));
    }

    @GetMapping("/list")
    @Operation(summary = "部门扁平列表（表单下拉）")
    @PreAuthorize("@ss.hasRead('system:dept:list')")
    public CommonResult<List<DeptDO>> list() {
        return CommonResult.success(deptService.listAll());
    }

    @GetMapping("/get")
    @Operation(summary = "部门详情")
    @PreAuthorize("@ss.hasPermission('system:dept:query')")
    public CommonResult<DeptDO> get(@RequestParam Long id) {
        return CommonResult.success(deptService.getById(id));
    }

    @Log(title = "部门管理", businessType = Log.BusinessType.INSERT)
    @PostMapping("/create")
    @Operation(summary = "新增部门")
    @PreAuthorize("@ss.hasPermission('system:dept:create')")
    public CommonResult<Long> create(@RequestBody DeptDO dept) {
        deptService.create(dept);
        return CommonResult.success(dept.getId());
    }

    @Log(title = "部门管理", businessType = Log.BusinessType.UPDATE)
    @PutMapping("/update")
    @Operation(summary = "修改部门")
    @PreAuthorize("@ss.hasPermission('system:dept:update')")
    public CommonResult<Boolean> update(@RequestBody DeptDO dept) {
        deptService.update(dept);
        return CommonResult.success(true);
    }

    @Log(title = "部门管理", businessType = Log.BusinessType.DELETE)
    @DeleteMapping("/delete")
    @Operation(summary = "删除部门")
    @PreAuthorize("@ss.hasPermission('system:dept:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        deptService.delete(id);
        return CommonResult.success(true);
    }

    @Log(title = "部门管理", businessType = Log.BusinessType.UPDATE)
    @PutMapping("/move")
    @Operation(summary = "拖拽移动部门")
    @PreAuthorize("@ss.hasPermission('system:dept:update')")
    public CommonResult<Boolean> move(
            @RequestParam Long id,
            @RequestParam Long parentId,
            @RequestParam(required = false) Integer sort) {
        deptService.move(id, parentId, sort);
        return CommonResult.success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新部门状态")
    @PreAuthorize("@ss.hasPermission('system:dept:update')")
    public CommonResult<Boolean> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        deptService.updateStatus(id, status);
        return CommonResult.success(true);
    }

    @GetMapping("/recycle/page")
    @Operation(summary = "部门回收站分页")
    @PreAuthorize("@ss.hasRecycleRead()")
    public CommonResult<PageResult<DeptDO>> recyclePage(PageParam pageParam,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        return CommonResult.success(deptService.recyclePage(pageParam, name, status));
    }

    @PutMapping("/restore")
    @Operation(summary = "恢复部门")
    @PreAuthorize("@ss.hasRecycleRestore('system:dept:delete')")
    public CommonResult<Boolean> restore(@RequestParam Long id) {
        deptService.restore(id);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete-permanent")
    @Operation(summary = "彻底删除部门")
    @PreAuthorize("@ss.hasRecycleDelete('system:dept:delete')")
    public CommonResult<Boolean> deletePermanent(@RequestParam Long id) {
        deptService.deletePermanent(id);
        return CommonResult.success(true);
    }
}

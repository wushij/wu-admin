package com.admin.server.modules.system.api.role;

import com.admin.server.framework.log.annotation.Log;
import com.admin.server.common.core.CommonResult;
import com.admin.server.common.core.PageParam;
import com.admin.server.common.core.PageResult;
import com.admin.server.common.util.BeanMappingUtils;
import com.admin.server.modules.system.api.role.vo.AssignMenuReqVO;
import com.admin.server.modules.system.api.role.vo.RoleCreateReqVO;
import com.admin.server.modules.system.api.role.vo.RoleRespVO;
import com.admin.server.modules.system.api.role.vo.RoleUpdateReqVO;
import com.admin.server.modules.system.dal.dataobject.permission.RoleDO;
import com.admin.server.modules.system.service.role.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Set;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/system/role")
public class RoleController {

    @Resource
    private RoleService roleService;

    @Operation(summary = "获取角色列表")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasRead('system:role:list')")
    public CommonResult<List<RoleRespVO>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        List<RoleDO> list = roleService.list(name, status);
        return CommonResult.success(BeanMappingUtils.copyListProperties(list, RoleRespVO.class));
    }

    @Operation(summary = "获取角色分页")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasRead('system:role:list')")
    public CommonResult<PageResult<RoleRespVO>> page(PageParam pageParam) {
        PageResult<RoleDO> page = roleService.page(pageParam);
        return CommonResult.success(BeanMappingUtils.copyPageProperties(page, RoleRespVO.class));
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('system:role:query')")
    public CommonResult<RoleRespVO> get(@RequestParam Long id) {
        RoleDO role = roleService.getById(id);
        return CommonResult.success(BeanMappingUtils.copyProperties(role, RoleRespVO.class));
    }

    @Log(title = "角色管理", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "新增角色")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('system:role:create')")
    public CommonResult<Long> create(@Validated @RequestBody RoleCreateReqVO reqVO) {
        return CommonResult.success(roleService.create(reqVO));
    }

    @Log(title = "角色管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "修改角色")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('system:role:update')")
    public CommonResult<Boolean> update(@Validated @RequestBody RoleUpdateReqVO reqVO) {
        roleService.update(reqVO);
        return CommonResult.success(true);
    }

    @Log(title = "角色管理", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除角色")
    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('system:role:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        roleService.delete(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "角色回收站分页")
    @GetMapping("/recycle/page")
    @PreAuthorize("@ss.hasRecycleRead()")
    public CommonResult<PageResult<RoleRespVO>> recyclePage(PageParam pageParam,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        PageResult<RoleDO> page = roleService.recyclePage(pageParam, name, status);
        return CommonResult.success(BeanMappingUtils.copyPageProperties(page, RoleRespVO.class));
    }

    @Operation(summary = "恢复角色")
    @PutMapping("/restore")
    @PreAuthorize("@ss.hasRecycleRestore('system:role:delete')")
    public CommonResult<Boolean> restore(@RequestParam Long id) {
        roleService.restore(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "彻底删除角色")
    @DeleteMapping("/delete-permanent")
    @PreAuthorize("@ss.hasRecycleDelete('system:role:delete')")
    public CommonResult<Boolean> deletePermanent(@RequestParam Long id) {
        roleService.deletePermanent(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "获取角色菜单列表")
    @GetMapping("/get-menu-ids")
    @PreAuthorize("@ss.hasPermission('system:role:query')")
    public CommonResult<Set<Long>> getMenuIds(@RequestParam Long roleId) {
        return CommonResult.success(roleService.getMenuIdsForAssign(roleId));
    }

    @Operation(summary = "分配角色菜单")
    @Log(title = "角色管理", businessType = Log.BusinessType.UPDATE)
    @PostMapping("/assign-menu")
    @PreAuthorize("@ss.hasPermission('system:role:update')")
    public CommonResult<Boolean> assignMenu(@Validated @RequestBody AssignMenuReqVO reqVO) {
        roleService.assignMenu(reqVO.getRoleId(), reqVO.getMenuIds());
        return CommonResult.success(true);
    }

    @Operation(summary = "更新角色状态")
    @PutMapping("/update-status")
    @PreAuthorize("@ss.hasPermission('system:role:update')")
    public CommonResult<Boolean> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        roleService.updateStatus(id, status);
        return CommonResult.success(true);
    }
}

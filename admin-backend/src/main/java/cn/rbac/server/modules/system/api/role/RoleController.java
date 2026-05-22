package cn.rbac.server.modules.system.api.role;

import cn.rbac.server.framework.log.annotation.Log;
import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.dal.dataobject.permission.RoleDO;
import cn.rbac.server.modules.system.dal.mysql.permission.RoleMapper;
import cn.rbac.server.modules.system.service.permission.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Set;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/system/role")
public class RoleController {
    
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private PermissionService permissionService;
    
    @Operation(summary = "获取角色列表")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('system:role:list')")
    public CommonResult<List<RoleDO>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<RoleDO> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(RoleDO::getName, name);
        }
        if (status != null) {
            wrapper.eq(RoleDO::getStatus, status);
        }
        List<RoleDO> roles = roleMapper.selectList(wrapper);
        // 填充菜单ID列表
        roles.forEach(role -> {
            Set<Long> menuIds = permissionService.getRoleMenuListByRoleId(role.getId());
            role.setMenuIds(menuIds);
        });
        return CommonResult.success(roles);
    }
    
    @Operation(summary = "获取角色分页")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('system:role:list')")
    public CommonResult<PageResult<RoleDO>> page(PageParam pageParam) {
        Page<RoleDO> page = roleMapper.selectPage(new Page<>(pageParam.getPageNo(), pageParam.getPageSize()), null);
        return CommonResult.success(PageResult.of(page.getRecords(), page.getTotal()));
    }
    
    @Operation(summary = "获取角色详情")
    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('system:role:query')")
    public CommonResult<RoleDO> get(@RequestParam Long id) {
        return CommonResult.success(roleMapper.selectById(id));
    }
    
    @Log(title = "角色管理", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "新增角色")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('system:role:create')")
    public CommonResult<Long> create(@RequestBody RoleCreateReqVO reqVO) {
        RoleDO role = new RoleDO();
        role.setName(reqVO.getName());
        role.setCode(reqVO.getCode());
        role.setSort(reqVO.getSort());
        role.setStatus(1);
        role.setRemark(reqVO.getRemark());
        roleMapper.insert(role);
        return CommonResult.success(role.getId());
    }
    
    @Log(title = "角色管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "修改角色")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('system:role:update')")
    public CommonResult<Boolean> update(@RequestBody RoleUpdateReqVO reqVO) {
        RoleDO role = roleMapper.selectById(reqVO.getId());
        role.setName(reqVO.getName());
        role.setCode(reqVO.getCode());
        role.setSort(reqVO.getSort());
        role.setStatus(reqVO.getStatus());
        role.setRemark(reqVO.getRemark());
        roleMapper.updateById(role);
        return CommonResult.success(true);
    }
    
    @Log(title = "角色管理", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除角色")
    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('system:role:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        roleMapper.deleteById(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "角色回收站分页")
    @GetMapping("/recycle/page")
    @PreAuthorize("@ss.hasPermission('system:role:delete')")
    public CommonResult<PageResult<RoleDO>> recyclePage(PageParam pageParam,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        Page<RoleDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<RoleDO> deletedPage = (Page<RoleDO>) roleMapper.selectDeletedPage(page, name, status);
        List<RoleDO> roles = deletedPage.getRecords();
        roles.forEach(role -> role.setMenuIds(permissionService.getRoleMenuListByRoleId(role.getId())));
        return CommonResult.success(PageResult.of(roles, deletedPage.getTotal()));
    }

    @Operation(summary = "恢复角色")
    @PutMapping("/restore")
    @PreAuthorize("@ss.hasPermission('system:role:delete')")
    public CommonResult<Boolean> restore(@RequestParam Long id) {
        int rows = roleMapper.restoreById(id);
        if (rows == 0) {
            return CommonResult.error(404, "回收站角色不存在");
        }
        return CommonResult.success(true);
    }

    @Operation(summary = "彻底删除角色")
    @DeleteMapping("/delete-permanent")
    @PreAuthorize("@ss.hasPermission('system:role:delete')")
    public CommonResult<Boolean> deletePermanent(@RequestParam Long id) {
        int rows = roleMapper.deletePhysicalById(id);
        if (rows == 0) {
            return CommonResult.error(404, "回收站角色不存在");
        }
        return CommonResult.success(true);
    }
    
    @Operation(summary = "获取角色菜单列表")
    @GetMapping("/get-menu-ids")
    public CommonResult<Set<Long>> getMenuIds(@RequestParam Long roleId) {
        return CommonResult.success(permissionService.getRoleMenuListByRoleId(roleId));
    }

    @Operation(summary = "分配角色菜单")
    @Log(title = "角色管理", businessType = Log.BusinessType.UPDATE)
    @PostMapping("/assign-menu")
    public CommonResult<Boolean> assignMenu(@RequestBody AssignMenuReqVO reqVO) {
        permissionService.assignRoleMenu(reqVO.getRoleId(), reqVO.getMenuIds());
        return CommonResult.success(true);
    }
    
    @Operation(summary = "更新角色状态")
    @PutMapping("/update-status")
    @PreAuthorize("@ss.hasPermission('system:role:update')")
    public CommonResult<Boolean> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        RoleDO role = roleMapper.selectById(id);
        role.setStatus(status);
        roleMapper.updateById(role);
        return CommonResult.success(true);
    }
    
    @Data
    public static class RoleCreateReqVO {
        private String name;
        private String code;
        private Integer sort;
        private String remark;
    }
    
    @Data
    public static class RoleUpdateReqVO {
        private Long id;
        private String name;
        private String code;
        private Integer sort;
        private Integer status;
        private String remark;
    }
    
    @Data
    public static class AssignMenuReqVO {
        private Long roleId;
        private Set<Long> menuIds;
    }
}

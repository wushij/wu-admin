package cn.rbac.server.modules.system.api.menu;

import cn.rbac.server.framework.log.annotation.Log;
import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.api.menu.vo.MenuCreateReqVO;
import cn.rbac.server.modules.system.api.menu.vo.MenuUpdateReqVO;
import cn.rbac.server.modules.system.dal.dataobject.permission.MenuDO;
import cn.rbac.server.modules.system.service.menu.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.util.List;

@Tag(name = "菜单管理")
@RestController
@RequestMapping("/system/menu")
public class MenuController {

    @Resource
    private MenuService menuService;

    @Operation(summary = "获取菜单树（管理页）")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasRead('system:menu:list')")
    public CommonResult<List<MenuDO>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer type) {
        return CommonResult.success(menuService.listTree(name, status, type));
    }

    @Operation(summary = "获取菜单全量列表（角色分配等，扁平）")
    @GetMapping("/simple-list")
    @PreAuthorize("@ss.hasRead('system:menu:list')")
    public CommonResult<List<MenuDO>> simpleList() {
        return CommonResult.success(menuService.listSimple());
    }

    @Operation(summary = "获取菜单详情")
    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('system:menu:query')")
    public CommonResult<MenuDO> get(@RequestParam Long id) {
        return CommonResult.success(menuService.getMenu(id));
    }

    @Log(title = "菜单管理", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "新增菜单")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('system:menu:create')")
    public CommonResult<Long> create(@RequestBody MenuCreateReqVO reqVO) {
        return CommonResult.success(menuService.createMenu(reqVO));
    }

    @Log(title = "菜单管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "修改菜单")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('system:menu:update')")
    public CommonResult<Boolean> update(@RequestBody MenuUpdateReqVO reqVO) {
        menuService.updateMenu(reqVO);
        return CommonResult.success(true);
    }

    @Log(title = "菜单管理", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除菜单")
    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('system:menu:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        menuService.deleteMenu(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "菜单回收站分页")
    @GetMapping("/recycle/page")
    @PreAuthorize("@ss.hasRecycleRead()")
    public CommonResult<PageResult<MenuDO>> recyclePage(PageParam pageParam,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        return CommonResult.success(menuService.recyclePage(pageParam, name, status));
    }

    @Operation(summary = "恢复菜单")
    @PutMapping("/restore")
    @PreAuthorize("@ss.hasRecycleRestore('system:menu:delete')")
    public CommonResult<Boolean> restore(@RequestParam Long id) {
        menuService.restoreMenu(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "彻底删除菜单")
    @DeleteMapping("/delete-permanent")
    @PreAuthorize("@ss.hasRecycleDelete('system:menu:delete')")
    public CommonResult<Boolean> deletePermanent(@RequestParam Long id) {
        menuService.deletePermanent(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "更新菜单状态")
    @PutMapping("/update-status")
    @PreAuthorize("@ss.hasPermission('system:menu:update')")
    public CommonResult<Boolean> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        menuService.updateStatus(id, status);
        return CommonResult.success(true);
    }
}

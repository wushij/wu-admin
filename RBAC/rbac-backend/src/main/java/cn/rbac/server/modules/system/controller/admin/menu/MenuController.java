package cn.rbac.server.modules.system.controller.admin.menu;

import cn.rbac.server.framework.log.annotation.Log;
import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.dal.dataobject.permission.MenuDO;
import cn.rbac.server.modules.system.dal.mysql.permission.MenuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

@Tag(name = "菜单管理")
@RestController
@RequestMapping("/system/menu")
public class MenuController {
    
    @Resource
    private MenuMapper menuMapper;
    
    @Operation(summary = "获取菜单列表")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('system:menu:list')")
    public CommonResult<List<MenuDO>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<MenuDO> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(MenuDO::getName, name);
        }
        if (status != null) {
            wrapper.eq(MenuDO::getStatus, status);
        }
        return CommonResult.success(menuMapper.selectList(wrapper));
    }
    
    @Operation(summary = "获取菜单详情")
    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('system:menu:query')")
    public CommonResult<MenuDO> get(@RequestParam Long id) {
        return CommonResult.success(menuMapper.selectById(id));
    }
    
    @Log(title = "菜单管理", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "新增菜单")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('system:menu:create')")
    public CommonResult<Long> create(@RequestBody MenuCreateReqVO reqVO) {
        MenuDO menu = new MenuDO();
        menu.setName(reqVO.getName());
        menu.setPermission(reqVO.getPermission());
        menu.setType(reqVO.getType());
        menu.setSort(reqVO.getSort());
        menu.setParentId(reqVO.getParentId());
        menu.setPath(reqVO.getPath());
        menu.setIcon(reqVO.getIcon());
        menu.setComponent(reqVO.getComponent());
        menu.setStatus(1);
        menuMapper.insert(menu);
        return CommonResult.success(menu.getId());
    }
    
    @Log(title = "菜单管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "修改菜单")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('system:menu:update')")
    public CommonResult<Boolean> update(@RequestBody MenuUpdateReqVO reqVO) {
        MenuDO menu = menuMapper.selectById(reqVO.getId());
        menu.setName(reqVO.getName());
        menu.setPermission(reqVO.getPermission());
        menu.setType(reqVO.getType());
        menu.setSort(reqVO.getSort());
        menu.setParentId(reqVO.getParentId());
        menu.setPath(reqVO.getPath());
        menu.setIcon(reqVO.getIcon());
        menu.setComponent(reqVO.getComponent());
        menu.setStatus(reqVO.getStatus());
        menuMapper.updateById(menu);
        return CommonResult.success(true);
    }
    
    @Log(title = "菜单管理", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除菜单")
    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('system:menu:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        menuMapper.deleteById(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "菜单回收站分页")
    @GetMapping("/recycle/page")
    @PreAuthorize("@ss.hasPermission('system:menu:delete')")
    public CommonResult<PageResult<MenuDO>> recyclePage(PageParam pageParam,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        Page<MenuDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<MenuDO> deletedPage = (Page<MenuDO>) menuMapper.selectDeletedPage(page, name, status);
        return CommonResult.success(PageResult.of(deletedPage.getRecords(), deletedPage.getTotal()));
    }

    @Operation(summary = "恢复菜单")
    @PutMapping("/restore")
    @PreAuthorize("@ss.hasPermission('system:menu:delete')")
    public CommonResult<Boolean> restore(@RequestParam Long id) {
        int rows = menuMapper.restoreById(id);
        if (rows == 0) {
            return CommonResult.error(404, "回收站菜单不存在");
        }
        return CommonResult.success(true);
    }

    @Operation(summary = "彻底删除菜单")
    @DeleteMapping("/delete-permanent")
    @PreAuthorize("@ss.hasPermission('system:menu:delete')")
    public CommonResult<Boolean> deletePermanent(@RequestParam Long id) {
        int rows = menuMapper.deletePhysicalById(id);
        if (rows == 0) {
            return CommonResult.error(404, "回收站菜单不存在");
        }
        return CommonResult.success(true);
    }
    
    @Operation(summary = "更新菜单状态")
    @PutMapping("/update-status")
    @PreAuthorize("@ss.hasPermission('system:menu:update')")
    public CommonResult<Boolean> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        MenuDO menu = menuMapper.selectById(id);
        menu.setStatus(status);
        menuMapper.updateById(menu);
        return CommonResult.success(true);
    }
    
    @Data
    public static class MenuCreateReqVO {
        private String name;
        private String permission;
        private Integer type;
        private Integer sort;
        private Long parentId;
        private String path;
        private String icon;
        private String component;
    }
    
    @Data
    public static class MenuUpdateReqVO {
        private Long id;
        private String name;
        private String permission;
        private Integer type;
        private Integer sort;
        private Long parentId;
        private String path;
        private String icon;
        private String component;
        private Integer status;
    }
}

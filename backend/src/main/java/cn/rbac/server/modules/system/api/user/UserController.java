package cn.rbac.server.modules.system.api.user;

import cn.rbac.server.framework.log.annotation.Log;
import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.api.user.vo.AssignRoleReqVO;
import cn.rbac.server.modules.system.api.user.vo.UserCreateReqVO;
import cn.rbac.server.modules.system.api.user.vo.UserUpdateReqVO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Set;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/system/user")
public class UserController {

    @Resource
    private UserService userService;

    @Operation(summary = "获取用户列表")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasRead('system:user:list')")
    public CommonResult<List<UserDO>> list() {
        return CommonResult.success(userService.listAll());
    }

    @Operation(summary = "获取用户分页")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasRead('system:user:list')")
    public CommonResult<PageResult<UserDO>> page(PageParam pageParam,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long postId,
            @RequestParam(required = false) Boolean loginLocked) {
        return CommonResult.success(userService.page(pageParam, keyword, username, mobile, status, deptId, postId, loginLocked));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<UserDO> get(@RequestParam Long id) {
        return CommonResult.success(userService.getDetail(id));
    }

    @Log(title = "用户管理", businessType = Log.BusinessType.INSERT, isSaveRequestData = false)
    @Operation(summary = "新增用户")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('system:user:create')")
    public CommonResult<Long> create(@Validated @RequestBody UserCreateReqVO reqVO) {
        return CommonResult.success(userService.createUser(reqVO));
    }

    @Log(title = "用户管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "修改用户")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> update(@Validated @RequestBody UserUpdateReqVO reqVO) {
        userService.updateUser(reqVO);
        return CommonResult.success(true);
    }

    @Log(title = "用户管理", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除用户")
    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('system:user:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        userService.deleteUser(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "用户回收站分页")
    @GetMapping("/recycle/page")
    @PreAuthorize("@ss.hasPermission('system:user:delete')")
    public CommonResult<PageResult<UserDO>> recyclePage(PageParam pageParam,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long deptId) {
        return CommonResult.success(userService.recyclePage(pageParam, username, mobile, status, deptId));
    }

    @Operation(summary = "恢复用户")
    @PutMapping("/restore")
    @PreAuthorize("@ss.hasPermission('system:user:delete')")
    public CommonResult<Boolean> restore(@RequestParam Long id) {
        userService.restore(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "彻底删除用户")
    @DeleteMapping("/delete-permanent")
    @PreAuthorize("@ss.hasPermission('system:user:delete')")
    public CommonResult<Boolean> deletePermanent(@RequestParam Long id) {
        userService.deletePermanent(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "获取用户角色列表")
    @GetMapping("/get-role-ids")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<Set<Long>> getRoleIds(@RequestParam Long userId) {
        return CommonResult.success(userService.getRoleIds(userId));
    }

    @Operation(summary = "分配用户角色")
    @PostMapping("/assign-role")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> assignRole(@Validated @RequestBody AssignRoleReqVO reqVO) {
        userService.assignRole(reqVO);
        return CommonResult.success(true);
    }

    @Operation(summary = "更新用户状态")
    @PutMapping("/update-status")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return CommonResult.success(true);
    }

    @Operation(summary = "重置用户密码")
    @Log(title = "用户管理", businessType = Log.BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    @PutMapping("/reset-password")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> resetPassword(@RequestParam Long id, @RequestParam String password) {
        userService.resetPassword(id, password);
        return CommonResult.success(true);
    }

    @Log(title = "用户管理", businessType = Log.BusinessType.UPDATE, isSaveRequestData = false)
    @Operation(summary = "解除登录失败锁定")
    @PutMapping("/unlock-login")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> unlockLogin(@RequestParam Long id) {
        userService.unlockLogin(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "踢用户下线")
    @DeleteMapping("/kick-out")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> kickOut(@RequestParam Long id) {
        userService.kickOut(id);
        return CommonResult.success(true);
    }
}

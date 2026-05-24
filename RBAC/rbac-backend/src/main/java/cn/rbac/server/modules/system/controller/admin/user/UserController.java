package cn.rbac.server.modules.system.controller.admin.user;

import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.framework.security.core.service.TokenService;
import cn.rbac.server.modules.system.dal.dataobject.dept.DeptDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.dal.mysql.dept.DeptMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.service.permission.PermissionService;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/system/user")
public class UserController {
    
    @Resource
    private UserMapper userMapper;
    @Resource
    private DeptMapper deptMapper;
    @Resource
    private PermissionService permissionService;
    @Resource
    private TokenService tokenService;
    
    @Operation(summary = "获取用户列表")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('system:user:list')")
    public CommonResult<List<UserDO>> list() {
        return CommonResult.success(userMapper.selectList(null));
    }
    
    @Operation(summary = "获取用户分页")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('system:user:list')")
    public CommonResult<PageResult<UserDO>> page(PageParam pageParam,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long deptId) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            wrapper.like(UserDO::getUsername, username);
        }
        if (mobile != null && !mobile.isEmpty()) {
            wrapper.like(UserDO::getMobile, mobile);
        }
        if (status != null) {
            wrapper.eq(UserDO::getStatus, status);
        }
        if (deptId != null) {
            wrapper.eq(UserDO::getDeptId, deptId);
        }
        Page<UserDO> page = userMapper.selectPage(new Page<>(pageParam.getPageNo(), pageParam.getPageSize()), wrapper);
        List<UserDO> users = page.getRecords();
        // 填充部门名称和角色ID
        if (!users.isEmpty()) {
            Set<Long> userIds = users.stream().map(UserDO::getId).collect(Collectors.toSet());
            // 填充部门名称
            Set<Long> deptIds = users.stream().map(UserDO::getDeptId).filter(id -> id != null).collect(Collectors.toSet());
            if (!deptIds.isEmpty()) {
                List<DeptDO> depts = deptMapper.selectBatchIds(deptIds);
                Map<Long, String> deptNameMap = depts.stream().collect(Collectors.toMap(DeptDO::getId, DeptDO::getName));
                users.forEach(user -> {
                    if (user.getDeptId() != null) {
                        user.setDeptName(deptNameMap.get(user.getDeptId()));
                    }
                });
            }
            // 填充角色ID列表
            users.forEach(user -> {
                Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(user.getId());
                user.setRoleIds(roleIds);
            });
        }
        return CommonResult.success(PageResult.of(page.getRecords(), page.getTotal()));
    }
    
    @Operation(summary = "获取用户详情")
    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<UserDO> get(@RequestParam Long id) {
        return CommonResult.success(userMapper.selectById(id));
    }
    
    @Operation(summary = "新增用户")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('system:user:create')")
    public CommonResult<Long> create(@RequestBody UserCreateReqVO reqVO) {
        UserDO user = new UserDO();
        user.setUsername(reqVO.getUsername());
        user.setPassword(BCrypt.hashpw(reqVO.getPassword()));
        user.setNickname(reqVO.getNickname());
        user.setMobile(reqVO.getMobile());
        user.setEmail(reqVO.getEmail());
        user.setStatus(1);
        user.setDeptId(reqVO.getDeptId());
        userMapper.insert(user);
        // 分配角色（单选模式）
        if (reqVO.getRoleId() != null) {
            permissionService.assignUserRole(user.getId(), java.util.Collections.singleton(reqVO.getRoleId()));
        }
        return CommonResult.success(user.getId());
    }
    
    @Operation(summary = "修改用户")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> update(@RequestBody UserUpdateReqVO reqVO) {
        UserDO user = userMapper.selectById(reqVO.getId());
        user.setNickname(reqVO.getNickname());
        user.setMobile(reqVO.getMobile());
        user.setEmail(reqVO.getEmail());
        user.setStatus(reqVO.getStatus());
        user.setDeptId(reqVO.getDeptId());
        userMapper.updateById(user);
        // 更新用户角色（单选模式）
        if (reqVO.getRoleId() != null) {
            permissionService.assignUserRole(user.getId(), java.util.Collections.singleton(reqVO.getRoleId()));
        }
        return CommonResult.success(true);
    }
    
    @Operation(summary = "删除用户")
    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('system:user:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        userMapper.deleteById(id);
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
        Page<UserDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<UserDO> deletedPage = (Page<UserDO>) userMapper.selectDeletedPage(page, username, mobile, status, deptId);
        List<UserDO> users = deletedPage.getRecords();
        if (!users.isEmpty()) {
            Set<Long> deptIds = users.stream().map(UserDO::getDeptId).filter(id -> id != null).collect(Collectors.toSet());
            if (!deptIds.isEmpty()) {
                List<DeptDO> depts = deptMapper.selectBatchIds(deptIds);
                Map<Long, String> deptNameMap = depts.stream().collect(Collectors.toMap(DeptDO::getId, DeptDO::getName));
                users.forEach(user -> {
                    if (user.getDeptId() != null) {
                        user.setDeptName(deptNameMap.get(user.getDeptId()));
                    }
                });
            }
            users.forEach(user -> user.setRoleIds(permissionService.getUserRoleIdListByUserId(user.getId())));
        }
        return CommonResult.success(PageResult.of(users, deletedPage.getTotal()));
    }

    @Operation(summary = "恢复用户")
    @PutMapping("/restore")
    @PreAuthorize("@ss.hasPermission('system:user:delete')")
    public CommonResult<Boolean> restore(@RequestParam Long id) {
        int rows = userMapper.restoreById(id);
        if (rows == 0) {
            return CommonResult.error(404, "回收站用户不存在");
        }
        return CommonResult.success(true);
    }

    @Operation(summary = "彻底删除用户")
    @DeleteMapping("/delete-permanent")
    @PreAuthorize("@ss.hasPermission('system:user:delete')")
    public CommonResult<Boolean> deletePermanent(@RequestParam Long id) {
        int rows = userMapper.deletePhysicalById(id);
        if (rows == 0) {
            return CommonResult.error(404, "回收站用户不存在");
        }
        return CommonResult.success(true);
    }
    
    @Operation(summary = "获取用户角色列表")
    @GetMapping("/get-role-ids")
    public CommonResult<Set<Long>> getRoleIds(@RequestParam Long userId) {
        return CommonResult.success(permissionService.getUserRoleIdListByUserId(userId));
    }

    @Operation(summary = "分配用户角色")
    @PostMapping("/assign-role")
    public CommonResult<Boolean> assignRole(@RequestBody AssignRoleReqVO reqVO) {
        permissionService.assignUserRole(reqVO.getUserId(), reqVO.getRoleIds());
        return CommonResult.success(true);
    }
    
    @Operation(summary = "更新用户状态")
    @PutMapping("/update-status")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        UserDO user = userMapper.selectById(id);
        user.setStatus(status);
        userMapper.updateById(user);
        return CommonResult.success(true);
    }
    
    @Operation(summary = "重置用户密码")
    @PutMapping("/reset-password")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> resetPassword(@RequestParam Long id, @RequestParam String password) {
        UserDO user = userMapper.selectById(id);
        user.setPassword(BCrypt.hashpw(password));
        userMapper.updateById(user);
        return CommonResult.success(true);
    }
    
    @Operation(summary = "踢用户下线")
    @DeleteMapping("/kick-out")
    public CommonResult<Boolean> kickOut(@RequestParam Long id) {
        tokenService.removeToken(id);
        return CommonResult.success(true);
    }
    
    @Data
    public static class UserCreateReqVO {
        private String username;
        private String password;
        private String nickname;
        private String mobile;
        private String email;
        private Long deptId;
        private Long roleId;
    }
    
    @Data
    public static class UserUpdateReqVO {
        private Long id;
        private String nickname;
        private String mobile;
        private String email;
        private Integer status;
        private Long deptId;
        private Long roleId;
    }
    
    @Data
    public static class AssignRoleReqVO {
        private Long userId;
        private Set<Long> roleIds;
    }
}

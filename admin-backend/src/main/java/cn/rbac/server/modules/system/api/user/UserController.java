package cn.rbac.server.modules.system.api.user;

import cn.rbac.server.framework.log.annotation.Log;
import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.framework.security.core.service.TokenService;
import cn.rbac.server.modules.system.dal.dataobject.dept.DeptDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.dal.dataobject.post.PostDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserPostDO;
import cn.rbac.server.modules.system.dal.mysql.dept.DeptMapper;
import cn.rbac.server.modules.system.dal.mysql.post.PostMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserPostMapper;
import cn.rbac.server.modules.system.service.permission.PermissionService;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.util.Collections;
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
    @Resource
    private UserPostMapper userPostMapper;
    @Resource
    private PostMapper postMapper;
    
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
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long postId) {
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
        if (postId != null) {
            List<Long> userIds = userPostMapper.selectUserIdsByPostId(postId);
            if (userIds.isEmpty()) {
                wrapper.eq(UserDO::getId, -1L);
            } else {
                wrapper.in(UserDO::getId, userIds);
            }
        }
        Page<UserDO> page = userMapper.selectPage(new Page<>(pageParam.getPageNo(), pageParam.getPageSize()), wrapper);
        List<UserDO> users = page.getRecords();
        fillUserDisplayFields(users);
        return CommonResult.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    private void fillUserDisplayFields(List<UserDO> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
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
        List<UserPostDO> allLinks = userPostMapper.selectList(null);
        Map<Long, List<Long>> userPostMap = allLinks.stream()
                .collect(Collectors.groupingBy(UserPostDO::getUserId,
                        Collectors.mapping(UserPostDO::getPostId, Collectors.toList())));
        Set<Long> postIds = allLinks.stream().map(UserPostDO::getPostId).collect(Collectors.toSet());
        Map<Long, String> postNameMap = postIds.isEmpty() ? Collections.emptyMap()
                : postMapper.selectBatchIds(postIds).stream()
                .collect(Collectors.toMap(PostDO::getId, PostDO::getPostName));
        users.forEach(user -> {
            user.setRoleIds(permissionService.getUserRoleIdListByUserId(user.getId()));
            List<Long> pids = userPostMap.getOrDefault(user.getId(), Collections.emptyList());
            user.setPostIds(pids);
            if (!pids.isEmpty()) {
                user.setPostNames(pids.stream()
                        .map(postNameMap::get)
                        .filter(name -> name != null)
                        .collect(Collectors.joining("、")));
            }
        });
    }
    
    @Operation(summary = "获取用户详情")
    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<UserDO> get(@RequestParam Long id) {
        UserDO user = userMapper.selectById(id);
        if (user != null) {
            fillUserDisplayFields(Collections.singletonList(user));
        }
        return CommonResult.success(user);
    }
    
    @Log(title = "用户管理", businessType = Log.BusinessType.INSERT, isSaveRequestData = false)
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
        saveUserPosts(user.getId(), reqVO.getPostIds());
        return CommonResult.success(user.getId());
    }
    
    @Log(title = "用户管理", businessType = Log.BusinessType.UPDATE)
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
        saveUserPosts(user.getId(), reqVO.getPostIds());
        return CommonResult.success(true);
    }

    private void saveUserPosts(Long userId, List<Long> postIds) {
        userPostMapper.deleteByUserId(userId);
        if (postIds == null || postIds.isEmpty()) {
            return;
        }
        for (Long postId : postIds) {
            if (postId == null) {
                continue;
            }
            UserPostDO link = new UserPostDO();
            link.setUserId(userId);
            link.setPostId(postId);
            userPostMapper.insert(link);
        }
    }
    
    @Log(title = "用户管理", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除用户")
    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('system:user:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        userPostMapper.deleteByUserId(id);
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
    @Log(title = "用户管理", businessType = Log.BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
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
        private List<Long> postIds;
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
        private List<Long> postIds;
    }
    
    @Data
    public static class AssignRoleReqVO {
        private Long userId;
        private Set<Long> roleIds;
    }
}

package cn.rbac.server.modules.system.api.auth;

import cn.dev33.satoken.stp.StpUtil;
import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.framework.log.annotation.Log;
import cn.rbac.server.framework.storage.LocalFileStorage;
import cn.rbac.server.modules.system.dal.dataobject.dept.DeptDO;
import cn.rbac.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import cn.rbac.server.modules.system.dal.dataobject.permission.RoleDO;
import cn.rbac.server.modules.system.dal.dataobject.post.PostDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.dal.mysql.dept.DeptMapper;
import cn.rbac.server.modules.system.dal.mysql.loginlog.LoginLogMapper;
import cn.rbac.server.modules.system.dal.mysql.permission.RoleMapper;
import cn.rbac.server.modules.system.dal.mysql.post.PostMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserPostMapper;
import cn.rbac.server.modules.system.service.config.SystemConfigHelper;
import cn.rbac.server.modules.system.service.permission.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "个人中心")
@RestController
@RequestMapping("/auth/profile")
public class ProfileController {

    private static final long AVATAR_MAX_BYTES = 2L * 1024 * 1024;
    private static final String AVATAR_PATH_PREFIX = "images/avatar/";

    @Resource
    private UserMapper userMapper;
    @Resource
    private DeptMapper deptMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private PostMapper postMapper;
    @Resource
    private UserPostMapper userPostMapper;
    @Resource
    private LoginLogMapper loginLogMapper;
    @Resource
    private PermissionService permissionService;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private LocalFileStorage localFileStorage;
    @Resource
    private SystemConfigHelper systemConfigHelper;

    @Operation(summary = "获取当前用户资料")
    @GetMapping
    public CommonResult<Map<String, Object>> getProfile() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            return CommonResult.error(404, "用户不存在");
        }
        return CommonResult.success(buildProfileMap(user));
    }

    @Operation(summary = "更新当前用户资料")
    @Log(title = "个人中心", businessType = Log.BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Boolean> updateProfile(@RequestBody ProfileUpdateReqVO reqVO) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            return CommonResult.error(404, "用户不存在");
        }

        if (StringUtils.hasText(reqVO.getNickname())) {
            user.setNickname(reqVO.getNickname().trim());
        }
        if (reqVO.getMobile() != null) {
            String mobile = reqVO.getMobile().trim();
            if (StringUtils.hasText(mobile)) {
                UserDO exist = userMapper.selectOne(new LambdaQueryWrapper<UserDO>()
                        .eq(UserDO::getMobile, mobile)
                        .ne(UserDO::getId, userId)
                        .eq(UserDO::getDeleted, 0));
                if (exist != null) {
                    return CommonResult.error(400, "手机号已被其他账号使用");
                }
            }
            user.setMobile(mobile);
        }
        if (reqVO.getEmail() != null) {
            String email = reqVO.getEmail().trim();
            if (StringUtils.hasText(email)) {
                UserDO exist = userMapper.selectOne(new LambdaQueryWrapper<UserDO>()
                        .eq(UserDO::getEmail, email)
                        .ne(UserDO::getId, userId)
                        .eq(UserDO::getDeleted, 0));
                if (exist != null) {
                    return CommonResult.error(400, "邮箱已被其他账号使用");
                }
            }
            user.setEmail(email);
        }
        if (reqVO.getAvatar() != null) {
            user.setAvatar(reqVO.getAvatar());
        }

        userMapper.updateById(user);
        return CommonResult.success(true);
    }

    @Operation(summary = "修改当前用户密码")
    @Log(title = "个人中心", businessType = Log.BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    @PutMapping("/password")
    public CommonResult<Boolean> changePassword(@RequestBody ChangePasswordReqVO reqVO) {
        if (!StringUtils.hasText(reqVO.getOldPassword()) || !StringUtils.hasText(reqVO.getNewPassword())) {
            return CommonResult.error(400, "请填写原密码和新密码");
        }

        int minLen = systemConfigHelper.getRegisterMinPasswordLength();
        if (reqVO.getNewPassword().length() < minLen) {
            return CommonResult.error(400, "新密码长度不能少于 " + minLen + " 位");
        }
        if (Objects.equals(reqVO.getOldPassword(), reqVO.getNewPassword())) {
            return CommonResult.error(400, "新密码不能与原密码相同");
        }
        if (StringUtils.hasText(reqVO.getConfirmPassword())
                && !Objects.equals(reqVO.getNewPassword(), reqVO.getConfirmPassword())) {
            return CommonResult.error(400, "两次输入的新密码不一致");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            return CommonResult.error(404, "用户不存在");
        }
        if (!passwordEncoder.matches(reqVO.getOldPassword(), user.getPassword())) {
            return CommonResult.error(400, "原密码不正确");
        }

        user.setPassword(passwordEncoder.encode(reqVO.getNewPassword()));
        userMapper.updateById(user);
        return CommonResult.success(true);
    }

    @Operation(summary = "上传头像")
    @Log(title = "个人中心", businessType = Log.BusinessType.UPDATE, isSaveRequestData = false)
    @PostMapping("/avatar")
    public CommonResult<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return CommonResult.error(400, "请选择图片文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return CommonResult.error(400, "仅支持上传图片作为头像");
        }
        if (file.getSize() > AVATAR_MAX_BYTES) {
            return CommonResult.error(400, "头像大小不能超过 2MB");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        String suffix = LocalFileStorage.getSuffix(file.getOriginalFilename());
        if (!StringUtils.hasText(suffix)) {
            suffix = ".jpg";
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;
        String storagePath = AVATAR_PATH_PREFIX + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        try {
            String url = localFileStorage.upload(file.getInputStream(), storagePath, fileName);
            UserDO user = userMapper.selectById(userId);
            if (user != null) {
                user.setAvatar(url);
                userMapper.updateById(user);
            }
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            return CommonResult.success(result);
        } catch (IOException e) {
            return CommonResult.error(500, "头像上传失败");
        }
    }

    @Operation(summary = "我的登录记录")
    @GetMapping("/login-logs")
    public CommonResult<PageResult<LoginLogDO>> myLoginLogs(PageParam pageParam) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            return CommonResult.error(404, "用户不存在");
        }
        Page<LoginLogDO> page = loginLogMapper.selectPage(
                new Page<>(pageParam.getPageNo(), pageParam.getPageSize()),
                loginLogScope(user).orderByDesc(LoginLogDO::getLoginTime));
        return CommonResult.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    /** 按 userId 或 username 匹配（兼容历史未写 userId 的登录日志） */
    private LambdaQueryWrapper<LoginLogDO> loginLogScope(UserDO user) {
        return new LambdaQueryWrapper<LoginLogDO>()
                .and(w -> w.eq(LoginLogDO::getUserId, user.getId())
                        .or()
                        .eq(LoginLogDO::getUsername, user.getUsername()));
    }

    private Map<String, Object> buildProfileMap(UserDO user) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("userId", user.getId());
        profile.put("username", user.getUsername());
        profile.put("nickname", user.getNickname());
        profile.put("mobile", user.getMobile());
        profile.put("email", user.getEmail());
        profile.put("avatar", user.getAvatar());
        profile.put("status", user.getStatus());
        profile.put("deptId", user.getDeptId());
        profile.put("createTime", user.getCreateTime());
        profile.put("updateTime", user.getUpdateTime());
        profile.put("minPasswordLength", systemConfigHelper.getRegisterMinPasswordLength());

        if (user.getDeptId() != null) {
            DeptDO dept = deptMapper.selectById(user.getDeptId());
            profile.put("deptName", dept != null ? dept.getName() : null);
        } else {
            profile.put("deptName", null);
        }

        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(user.getId());
        profile.put("roleIds", roleIds);
        if (!roleIds.isEmpty()) {
            List<RoleDO> roles = roleMapper.selectBatchIds(roleIds);
            profile.put("roleNames", roles.stream().map(RoleDO::getName).collect(Collectors.toList()));
            profile.put("roleCodes", roles.stream().map(RoleDO::getCode).collect(Collectors.toList()));
        } else {
            profile.put("roleNames", Collections.emptyList());
            profile.put("roleCodes", Collections.emptyList());
        }

        List<Long> postIds = userPostMapper.selectPostIdsByUserId(user.getId());
        profile.put("postIds", postIds);
        if (!postIds.isEmpty()) {
            List<PostDO> posts = postMapper.selectBatchIds(postIds);
            profile.put("postNames", posts.stream().map(PostDO::getPostName).collect(Collectors.toList()));
        } else {
            profile.put("postNames", Collections.emptyList());
        }

        LoginLogDO lastLogin = loginLogMapper.selectOne(loginLogScope(user)
                .eq(LoginLogDO::getStatus, 0)
                .orderByDesc(LoginLogDO::getLoginTime)
                .last("LIMIT 1"));
        if (lastLogin != null) {
            profile.put("lastLoginTime", lastLogin.getLoginTime());
            profile.put("lastLoginIp", lastLogin.getIpaddr());
            profile.put("lastLoginLocation", lastLogin.getLoginLocation());
        }

        return profile;
    }

    @Data
    public static class ProfileUpdateReqVO {
        private String nickname;
        private String mobile;
        private String email;
        private String avatar;
    }

    @Data
    public static class ChangePasswordReqVO {
        private String oldPassword;
        private String newPassword;
        private String confirmPassword;
    }
}

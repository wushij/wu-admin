package com.admin.server.modules.system.service.auth.impl;

import com.admin.server.common.pojo.BusinessException;
import com.admin.server.common.pojo.PageParam;
import com.admin.server.common.pojo.PageResult;
import com.admin.server.framework.storage.LocalFileStorage;
import com.admin.server.modules.system.api.auth.vo.*;
import com.admin.server.modules.system.dal.dataobject.dept.DeptDO;
import com.admin.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import com.admin.server.modules.system.dal.dataobject.permission.RoleDO;
import com.admin.server.modules.system.dal.dataobject.post.PostDO;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;
import com.admin.server.modules.system.dal.mysql.dept.DeptMapper;
import com.admin.server.modules.system.dal.mysql.loginlog.LoginLogMapper;
import com.admin.server.modules.system.dal.mysql.permission.RoleMapper;
import com.admin.server.modules.system.dal.mysql.post.PostMapper;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;
import com.admin.server.modules.system.dal.mysql.user.UserPostMapper;
import com.admin.server.modules.system.service.auth.ProfileService;
import com.admin.server.modules.system.service.auth.ProfileSmsMobileBindService;
import com.admin.server.modules.system.service.auth.ProfileSmsPasswordService;
import com.admin.server.modules.system.service.dept.DeptService;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.admin.server.modules.system.service.permission.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ProfileServiceImpl implements ProfileService {

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
    @Resource
    private ProfileSmsPasswordService profileSmsPasswordService;
    @Resource
    private ProfileSmsMobileBindService profileSmsMobileBindService;
    @Resource
    private DeptService deptService;

    @Override
    public Map<String, Object> getProfile(Long userId) {
        UserDO user = requireUser(userId);
        return buildProfileMap(user);
    }

    @Override
    public void updateProfile(Long userId, ProfileUpdateReqVO reqVO) {
        UserDO user = requireUser(userId);
        String previousNickname = user.getNickname();
        if (StringUtils.hasText(reqVO.getNickname())) {
            user.setNickname(reqVO.getNickname().trim());
        }
        if (reqVO.getEmail() != null) {
            String email = reqVO.getEmail().trim();
            if (StringUtils.hasText(email)) {
                UserDO exist = userMapper.selectOne(new LambdaQueryWrapper<UserDO>()
                        .eq(UserDO::getEmail, email)
                        .ne(UserDO::getId, userId)
                        .eq(UserDO::getDeleted, 0));
                if (exist != null) {
                    throw new BusinessException(400, "邮箱已被其他账号使用");
                }
            }
            user.setEmail(email);
        }
        if (reqVO.getAvatar() != null) {
            user.setAvatar(reqVO.getAvatar());
        }
        userMapper.updateById(user);
        if (StringUtils.hasText(reqVO.getNickname())) {
            deptService.syncLeaderByUserId(userId, previousNickname, user.getNickname());
        }
    }

    @Override
    public String sendMobileBindSmsCode(Long userId, ProfileMobileBindSmsCodeReqVO reqVO, String clientIp) {
        return profileSmsMobileBindService.sendBindCode(
                userId, reqVO.getMobile(), clientIp, reqVO.getUuid(), reqVO.getCode());
    }

    @Override
    public void bindMobile(Long userId, ProfileMobileBindReqVO reqVO) {
        UserDO user = requireUser(userId);
        String err = profileSmsMobileBindService.bindMobile(userId, user, reqVO.getMobile(), reqVO.getSmsCode());
        if (err != null) {
            throw new BusinessException(400, err);
        }
        userMapper.updateById(user);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordReqVO reqVO) {
        if (!StringUtils.hasText(reqVO.getOldPassword()) || !StringUtils.hasText(reqVO.getNewPassword())) {
            throw new BusinessException(400, "请填写原密码和新密码");
        }
        int minLen = systemConfigHelper.getRegisterMinPasswordLength();
        if (reqVO.getNewPassword().length() < minLen) {
            throw new BusinessException(400, "新密码长度不能少于 " + minLen + " 位");
        }
        if (Objects.equals(reqVO.getOldPassword(), reqVO.getNewPassword())) {
            throw new BusinessException(400, "新密码不能与原密码相同");
        }
        if (StringUtils.hasText(reqVO.getConfirmPassword())
                && !Objects.equals(reqVO.getNewPassword(), reqVO.getConfirmPassword())) {
            throw new BusinessException(400, "两次输入的新密码不一致");
        }
        UserDO user = requireUser(userId);
        if (!passwordEncoder.matches(reqVO.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(reqVO.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    public String sendPasswordResetSmsCode(Long userId, ProfilePasswordSmsCodeReqVO reqVO, String clientIp) {
        UserDO user = requireUser(userId);
        return profileSmsPasswordService.sendResetCode(user, clientIp, reqVO.getUuid(), reqVO.getCode());
    }

    @Override
    public void resetPasswordBySms(Long userId, ProfilePasswordSmsResetReqVO reqVO) {
        UserDO user = requireUser(userId);
        String err = profileSmsPasswordService.resetPasswordBySms(
                user, reqVO.getSmsCode(), reqVO.getNewPassword(), reqVO.getConfirmPassword());
        if (err != null) {
            throw new BusinessException(400, err);
        }
        userMapper.updateById(user);
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择图片文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(400, "仅支持上传图片作为头像");
        }
        if (file.getSize() > AVATAR_MAX_BYTES) {
            throw new BusinessException(400, "头像大小不能超过 2MB");
        }
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
            return url;
        } catch (IOException e) {
            throw new BusinessException(500, "头像上传失败");
        }
    }

    @Override
    public PageResult<LoginLogDO> myLoginLogs(Long userId, PageParam pageParam) {
        UserDO user = requireUser(userId);
        Page<LoginLogDO> page = loginLogMapper.selectPage(
                new Page<>(pageParam.getPageNo(), pageParam.getPageSize()),
                loginLogScope(user).orderByDesc(LoginLogDO::getLoginTime));
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    private UserDO requireUser(Long userId) {
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

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
            List<RoleDO> roles = roleMapper.selectByIds(roleIds);
            if (roles != null) {
                profile.put("roleNames", roles.stream().map(RoleDO::getName).toList());
                profile.put("roleCodes", roles.stream().map(RoleDO::getCode).toList());
            } else {
                profile.put("roleNames", Collections.emptyList());
                profile.put("roleCodes", Collections.emptyList());
            }
        } else {
            profile.put("roleNames", Collections.emptyList());
            profile.put("roleCodes", Collections.emptyList());
        }

        List<Long> postIds = userPostMapper.selectPostIdsByUserId(user.getId());
        profile.put("postIds", postIds);
        if (!postIds.isEmpty()) {
            List<PostDO> posts = postMapper.selectByIds(postIds);
            if (posts != null) {
                profile.put("postNames", posts.stream().map(PostDO::getPostName).toList());
            } else {
                profile.put("postNames", Collections.emptyList());
            }
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
}

package com.admin.server.modules.system.service.auth.impl;

import com.admin.server.common.pojo.BusinessException;
import com.admin.server.modules.system.api.auth.vo.ChangePasswordReqVO;
import com.admin.server.modules.system.api.auth.vo.ProfileMobileBindReqVO;
import com.admin.server.modules.system.api.auth.vo.ProfileUpdateReqVO;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;
import com.admin.server.modules.system.dal.mysql.dept.DeptMapper;
import com.admin.server.modules.system.dal.mysql.loginlog.LoginLogMapper;
import com.admin.server.modules.system.dal.mysql.permission.RoleMapper;
import com.admin.server.modules.system.dal.mysql.post.PostMapper;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;
import com.admin.server.modules.system.dal.mysql.user.UserPostMapper;
import com.admin.server.modules.system.service.auth.ProfileSmsMobileBindService;
import com.admin.server.modules.system.service.auth.ProfileSmsPasswordService;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.admin.server.modules.system.service.permission.PermissionService;
import com.admin.server.testsupport.MybatisLambdaTestBase;
import com.admin.server.testsupport.MybatisMockMatchers;
import com.admin.server.testsupport.ServiceTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileServiceImpl 单元测试")
class ProfileServiceImplTest extends MybatisLambdaTestBase {

    private static final long USER_ID = 1L;

    @Mock
    private UserMapper userMapper;
    @Mock
    private DeptMapper deptMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private UserPostMapper userPostMapper;
    @Mock
    private LoginLogMapper loginLogMapper;
    @Mock
    private PermissionService permissionService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SystemConfigHelper systemConfigHelper;
    @Mock
    private ProfileSmsPasswordService profileSmsPasswordService;
    @Mock
    private ProfileSmsMobileBindService profileSmsMobileBindService;

    @InjectMocks
    private ProfileServiceImpl profileService;

    @Test
    @DisplayName("getProfile：用户不存在抛 404")
    void getProfile_userNotFound() {
        when(userMapper.selectById(USER_ID)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> profileService.getProfile(USER_ID));

        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("getProfile：返回基本资料字段")
    void getProfile_returnsBasicFields() {
        UserDO user = ServiceTestFixtures.user(USER_ID, "alice", 1);
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(systemConfigHelper.getRegisterMinPasswordLength()).thenReturn(6);
        when(permissionService.getUserRoleIdListByUserId(USER_ID)).thenReturn(Collections.emptySet());
        when(userPostMapper.selectPostIdsByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(loginLogMapper.selectOne(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(null);

        Map<String, Object> profile = profileService.getProfile(USER_ID);

        assertEquals(USER_ID, profile.get("userId"));
        assertEquals("alice", profile.get("username"));
        assertEquals(6, profile.get("minPasswordLength"));
    }

    @Test
    @DisplayName("updateProfile：邮箱已被占用抛 400")
    void updateProfile_duplicateEmail() {
        UserDO user = ServiceTestFixtures.user(USER_ID, "alice", 1);
        UserDO other = ServiceTestFixtures.user(2L, "bob", 1);
        other.setEmail("dup@test.com");
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(userMapper.selectOne(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(other);

        ProfileUpdateReqVO req = new ProfileUpdateReqVO();
        req.setEmail("dup@test.com");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> profileService.updateProfile(USER_ID, req));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("邮箱"));
        verify(userMapper, never()).updateById(any(UserDO.class));
    }

    @Test
    @DisplayName("changePassword：原密码不正确")
    void changePassword_wrongOldPassword() {
        UserDO user = ServiceTestFixtures.user(USER_ID, "alice", 1);
        when(systemConfigHelper.getRegisterMinPasswordLength()).thenReturn(6);
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

        ChangePasswordReqVO req = new ChangePasswordReqVO();
        req.setOldPassword("wrong");
        req.setNewPassword("newpass1");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> profileService.changePassword(USER_ID, req));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("原密码"));
    }

    @Test
    @DisplayName("changePassword：成功更新密码")
    void changePassword_success() {
        UserDO user = ServiceTestFixtures.user(USER_ID, "alice", 1);
        when(systemConfigHelper.getRegisterMinPasswordLength()).thenReturn(6);
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(passwordEncoder.matches("oldpass", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newpass1")).thenReturn("$2a$10$new");

        ChangePasswordReqVO req = new ChangePasswordReqVO();
        req.setOldPassword("oldpass");
        req.setNewPassword("newpass1");

        profileService.changePassword(USER_ID, req);

        verify(userMapper).updateById(argThat((UserDO u) -> "$2a$10$new".equals(u.getPassword())));
    }

    @Test
    @DisplayName("bindMobile：短信验证失败抛 400")
    void bindMobile_smsError() {
        UserDO user = ServiceTestFixtures.user(USER_ID, "alice", 1);
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(profileSmsMobileBindService.bindMobile(eq(USER_ID), eq(user), anyString(), anyString()))
                .thenReturn("验证码错误");

        ProfileMobileBindReqVO req = new ProfileMobileBindReqVO();
        req.setMobile("13800138000");
        req.setSmsCode("000000");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> profileService.bindMobile(USER_ID, req));

        assertEquals(400, ex.getCode());
        verify(userMapper, never()).updateById(any(UserDO.class));
    }
}

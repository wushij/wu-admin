package com.admin.server.modules.system.service.user.impl;

import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.system.enums.ErrorCodeConstants;
import com.admin.server.framework.security.core.service.TokenService;
import com.admin.server.modules.system.api.user.vo.UserCreateReqVO;
import com.admin.server.modules.system.api.user.vo.UserUpdateReqVO;
import com.admin.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;
import com.admin.server.modules.system.dal.mysql.dept.DeptMapper;
import com.admin.server.modules.system.dal.mysql.loginlog.LoginLogMapper;
import com.admin.server.modules.system.dal.mysql.post.PostMapper;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;
import com.admin.server.modules.system.dal.mysql.user.UserPostMapper;
import com.admin.server.modules.system.service.auth.LoginLockService;
import com.admin.server.modules.system.service.permission.PermissionService;
import com.admin.server.testsupport.MybatisLambdaTestBase;
import com.admin.server.testsupport.ServiceTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl 单元测试")
class UserServiceImplTest extends MybatisLambdaTestBase {

    @Mock
    private UserMapper userMapper;
    @Mock
    private DeptMapper deptMapper;
    @Mock
    private PermissionService permissionService;
    @Mock
    private TokenService tokenService;
    @Mock
    private UserPostMapper userPostMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private LoginLockService loginLockService;
    @Mock
    private LoginLogMapper loginLogMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("createUser：创建用户并分配角色")
    void createUser_assignsRole() {
        UserCreateReqVO req = new UserCreateReqVO();
        req.setUsername("bob");
        req.setPassword("secret123");
        req.setNickname("Bob");
        req.setRoleId(2L);

        when(passwordEncoder.encode("secret123")).thenReturn("$2a$encoded");
        doAnswer(inv -> {
            UserDO user = inv.getArgument(0);
            user.setId(10L);
            return 1;
        }).when(userMapper).insert(any(UserDO.class));

        Long id = userService.createUser(req);

        assertEquals(10L, id);
        verify(permissionService).assignUserRole(10L, Set.of(2L));
        verify(userPostMapper).deleteByUserId(10L);
    }

    @Test
    @DisplayName("updateUser：用户不存在抛 404")
    void updateUser_notFound() {
        UserUpdateReqVO req = new UserUpdateReqVO();
        req.setId(99L);
        req.setNickname("n");
        when(userMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.updateUser(req));

        assertEquals(ErrorCodeConstants.USER_NOT_EXISTS.code(), ex.getCode());
        verify(userMapper, never()).updateById(any(UserDO.class));
    }

    @Test
    @DisplayName("updateStatus：用户不存在抛 404")
    void updateStatus_notFound() {
        when(userMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateStatus(1L, 0));

        assertEquals(ErrorCodeConstants.USER_NOT_EXISTS.code(), ex.getCode());
    }

    @Test
    @DisplayName("resetPassword：重置密码并加密")
    void resetPassword_encodesAndUpdates() {
        UserDO user = ServiceTestFixtures.user(1L, "alice", 1);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.encode("newPass")).thenReturn("$2a$new");

        userService.resetPassword(1L, "newPass");

        verify(userMapper).updateById(argThat((UserDO u) -> "$2a$new".equals(u.getPassword())));
    }

    @Test
    @DisplayName("restore：回收站用户不存在抛 404")
    void restore_notFound() {
        when(userMapper.restoreById(5L)).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.restore(5L));

        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("kickOut：调用 TokenService 踢出用户")
    void kickOut_delegatesToTokenService() {
        userService.kickOut(8L);
        verify(tokenService).removeToken(8L);
    }

    @Test
    @DisplayName("unlockLogin：用户不存在抛 404")
    void unlockLogin_notFound() {
        when(userMapper.selectById(99L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.unlockLogin(99L));
        assertEquals(ErrorCodeConstants.USER_NOT_EXISTS.code(), ex.getCode());
    }

    @Test
    @DisplayName("unlockLogin：解除账号并清除最近登录 IP 锁定")
    void unlockLogin_success() {
        UserDO user = ServiceTestFixtures.user(1L, "alice", 1);
        when(userMapper.selectById(1L)).thenReturn(user);
        LoginLogDO log = new LoginLogDO();
        log.setUsername("alice");
        log.setIpaddr("192.168.1.10");
        when(loginLogMapper.selectList(any())).thenReturn(List.of(log));

        userService.unlockLogin(1L);

        verify(loginLockService).unlockUser("alice");
        verify(loginLockService).unlockIp("192.168.1.10");
    }
}

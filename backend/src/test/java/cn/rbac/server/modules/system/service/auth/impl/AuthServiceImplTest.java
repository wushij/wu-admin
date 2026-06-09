package cn.rbac.server.modules.system.service.auth.impl;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.modules.system.api.auth.vo.LoginReqVO;
import cn.rbac.server.modules.system.api.auth.vo.RegisterReqVO;
import cn.rbac.server.modules.system.api.auth.vo.SmsCodeReqVO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.dal.mysql.permission.RoleMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.service.approval.RegisterApprovalService;
import cn.rbac.server.modules.system.service.config.SystemConfigHelper;
import cn.rbac.server.modules.system.service.loginlog.LoginLogService;
import cn.rbac.server.modules.system.service.monitor.OnlineUserService;
import cn.rbac.server.modules.system.service.permission.PermissionService;
import cn.rbac.server.modules.system.sms.AliyunDypnsSmsVerifyService;
import cn.rbac.server.modules.system.sms.SmsServiceFactory;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.rbac.server.framework.security.core.service.TokenService;
import cn.rbac.server.testsupport.MybatisLambdaTestBase;
import cn.rbac.server.testsupport.MybatisMockMatchers;
import cn.rbac.server.testsupport.ServiceTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthServiceImpl 单元测试")
class AuthServiceImplTest extends MybatisLambdaTestBase {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;
    @Mock
    private PermissionService permissionService;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private LoginLogService loginLogService;
    @Mock
    private OnlineUserService onlineUserService;
    @Mock
    private SystemConfigHelper systemConfigHelper;
    @Mock
    private RegisterApprovalService registerApprovalService;
    @Mock
    private SmsServiceFactory smsServiceFactory;
    @Mock
    private AliyunDypnsSmsVerifyService aliyunDypnsSmsVerifyService;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void stubCommonLoginGuards() {
        when(systemConfigHelper.getLoginPerIpMinute()).thenReturn(0);
        when(systemConfigHelper.isCaptchaEnabled()).thenReturn(false);

        RAtomicLong counter = mock(RAtomicLong.class);
        when(counter.incrementAndGet()).thenReturn(1L);
        when(redissonClient.getAtomicLong(anyString())).thenReturn(counter);

        @SuppressWarnings("unchecked")
        RBucket<Object> bucket = mock(RBucket.class);
        when(bucket.get()).thenReturn(null);
        when(redissonClient.getBucket(anyString())).thenReturn(bucket);
    }

    @Test
    @DisplayName("loginByAccount：停用账号拒绝登录")
    void loginByAccount_disabledUserRejected() {
        LoginReqVO req = loginReq("alice", "secret");
        UserDO user = ServiceTestFixtures.user(1L, "alice", 0);
        when(userMapper.selectOne(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(user);
        when(passwordEncoder.matches("secret", user.getPassword())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.loginByAccount(req, "127.0.0.1", "JUnit"));

        assertEquals(403, ex.getCode());
        assertTrue(ex.getMessage().contains("停用"));
        verify(loginLogService).recordAsync(any());
    }

    @Test
    @DisplayName("loginByAccount：密码错误返回统一提示")
    void loginByAccount_wrongPassword() {
        LoginReqVO req = loginReq("alice", "wrong");
        UserDO user = ServiceTestFixtures.user(1L, "alice", 1);
        when(userMapper.selectOne(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(user);
        when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.loginByAccount(req, "127.0.0.1", "JUnit"));

        assertEquals(400, ex.getCode());
        assertEquals("账号或密码错误", ex.getMessage());
    }

    @Test
    @DisplayName("loginByAccount：账号被锁定时拒绝登录")
    void loginByAccount_lockedAccountRejected() {
        LoginReqVO req = loginReq("alice", "secret");

        @SuppressWarnings("unchecked")
        RBucket<Object> lockBucket = mock(RBucket.class);
        when(lockBucket.get()).thenReturn(System.currentTimeMillis());
        when(lockBucket.remainTimeToLive()).thenReturn(120_000L);
        when(redissonClient.getBucket(contains("lock:user:alice"))).thenReturn(lockBucket);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.loginByAccount(req, "127.0.0.1", "JUnit"));

        assertEquals(429, ex.getCode());
        assertTrue(ex.getMessage().contains("锁定"));
    }

    @Test
    @DisplayName("loginByAccount：成功登录返回用户信息（Token 写入 Cookie）")
    void loginByAccount_success() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            SaSession session = mock(SaSession.class);
            stp.when(StpUtil::getSession).thenReturn(session);

            LoginReqVO req = loginReq("alice", "secret");
            UserDO user = ServiceTestFixtures.user(1L, "alice", 1);
            when(userMapper.selectOne(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(user);
            when(passwordEncoder.matches("secret", user.getPassword())).thenReturn(true);
            when(tokenService.createToken(1L, "alice")).thenReturn("token-abc");

            Map<String, Object> result = authService.loginByAccount(req, "127.0.0.1", "JUnit");

            assertNull(result.get("token"));
            assertEquals(1L, result.get("userId"));
            assertEquals("alice", result.get("username"));
            verify(tokenService).createToken(1L, "alice");
            verify(onlineUserService).recordLoginSession(eq(1L), eq("alice"), any(), eq("127.0.0.1"), eq("JUnit"));
            verify(loginLogService).recordAsync(any());
        }
    }

    @Test
    @DisplayName("register：用户名已存在")
    void register_usernameExists() {
        when(systemConfigHelper.isRegisterEnabled()).thenReturn(true);
        when(systemConfigHelper.getRegisterPerIpMinute()).thenReturn(0);
        when(systemConfigHelper.getRegisterMinPasswordLength()).thenReturn(6);
        when(systemConfigHelper.isRegisterCaptchaEnabled()).thenReturn(false);
        when(userMapper.selectByUsernameRaw("bob")).thenReturn(ServiceTestFixtures.user(1L, "bob", 1));

        RegisterReqVO req = new RegisterReqVO();
        req.setUsername("bob");
        req.setPassword("secret123");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(req, "127.0.0.1"));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("已存在"));
    }

    @Test
    @DisplayName("register：成功注册无需审核")
    void register_success() {
        when(systemConfigHelper.isRegisterEnabled()).thenReturn(true);
        when(systemConfigHelper.getRegisterPerIpMinute()).thenReturn(0);
        when(systemConfigHelper.getRegisterMinPasswordLength()).thenReturn(6);
        when(systemConfigHelper.isRegisterCaptchaEnabled()).thenReturn(false);
        when(systemConfigHelper.isRegisterNeedAudit()).thenReturn(false);
        when(systemConfigHelper.getRegisterDefaultRoleCode()).thenReturn("default");
        when(userMapper.selectByUsernameRaw("bob")).thenReturn(null);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$enc");
        when(roleMapper.selectOne(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(null);
        doAnswer(inv -> {
            UserDO u = inv.getArgument(0);
            u.setId(10L);
            return 1;
        }).when(userMapper).insert(any(UserDO.class));

        RegisterReqVO req = new RegisterReqVO();
        req.setUsername("bob");
        req.setPassword("secret123");

        assertFalse(authService.register(req, "127.0.0.1"));
        verify(userMapper).insert(argThat((UserDO u) -> u.getStatus() == 1));
        verify(registerApprovalService, never()).createOnRegister(any());
    }

    @Test
    @DisplayName("loginBySms：手机号未绑定账号")
    void loginBySms_phoneNotBound() {
        when(systemConfigHelper.isSmsLoginEnabled()).thenReturn(true);
        when(systemConfigHelper.isSmsEnabled()).thenReturn(true);
        when(userMapper.selectOne(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(null);

        LoginReqVO req = new LoginReqVO();
        req.setPhone("13800138000");
        req.setCode("123456");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.loginBySms(req, "127.0.0.1", "JUnit"));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("未绑定"));
    }

    @Test
    @DisplayName("loginBySms：验证码正确时登录成功")
    void loginBySms_success() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            SaSession session = mock(SaSession.class);
            stp.when(StpUtil::getSession).thenReturn(session);

            when(systemConfigHelper.isSmsLoginEnabled()).thenReturn(true);
            when(systemConfigHelper.isSmsEnabled()).thenReturn(true);
            when(systemConfigHelper.isAliyunAuthSmsProvider()).thenReturn(false);

            UserDO user = ServiceTestFixtures.user(1L, "alice", 1);
            user.setMobile("13800138000");
            when(userMapper.selectOne(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(user);

            @SuppressWarnings("unchecked")
            RBucket<Object> smsBucket = mock(RBucket.class);
            when(smsBucket.get()).thenReturn("123456");
            when(redissonClient.getBucket(contains("sms:login:"))).thenReturn(smsBucket);

            when(tokenService.createToken(1L, "alice")).thenReturn("sms-token");

            LoginReqVO req = new LoginReqVO();
            req.setPhone("13800138000");
            req.setCode("123456");

            Map<String, Object> result = authService.loginBySms(req, "127.0.0.1", "JUnit");

            assertNull(result.get("token"));
            verify(smsBucket).delete();
        }
    }

    @Test
    @DisplayName("sendSmsCode：发送间隔过短触发 429")
    void sendSmsCode_rateLimited() {
        when(systemConfigHelper.isSmsEnabled()).thenReturn(true);
        when(systemConfigHelper.isSmsLoginEnabled()).thenReturn(true);
        when(systemConfigHelper.getSmsPerIpMinute()).thenReturn(0);
        when(systemConfigHelper.getSmsPerIpDaily()).thenReturn(0);
        when(systemConfigHelper.getSmsPerPhoneDaily()).thenReturn(0);
        when(systemConfigHelper.getSmsSendIntervalSeconds()).thenReturn(60);

        @SuppressWarnings("unchecked")
        RBucket<Object> intervalBucket = mock(RBucket.class);
        when(intervalBucket.isExists()).thenReturn(true);
        when(redissonClient.getBucket(contains("sms:limit:"))).thenReturn(intervalBucket);

        SmsCodeReqVO req = new SmsCodeReqVO();
        req.setPhone("13800138000");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.sendSmsCode(req, "10.0.0.1"));

        assertEquals(429, ex.getCode());
        assertTrue(ex.getMessage().contains("频繁"));
    }

    @Test
    @DisplayName("loginByAccount：IP 限流触发 429")
    void loginByAccount_rateLimited() {
        when(systemConfigHelper.getLoginPerIpMinute()).thenReturn(5);
        RAtomicLong counter = mock(RAtomicLong.class);
        when(counter.incrementAndGet()).thenReturn(6L);
        when(redissonClient.getAtomicLong(contains("auth:rl:login"))).thenReturn(counter);

        LoginReqVO req = loginReq("alice", "secret");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.loginByAccount(req, "10.0.0.1", "JUnit"));

        assertEquals(429, ex.getCode());
        assertTrue(ex.getMessage().contains("频繁"));
    }

    private static LoginReqVO loginReq(String username, String password) {
        LoginReqVO req = new LoginReqVO();
        req.setUsername(username);
        req.setPassword(password);
        return req;
    }
}

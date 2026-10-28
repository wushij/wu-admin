package com.admin.server.modules.infra.service.monitor.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.framework.security.core.service.TokenService;
import com.admin.server.modules.infra.api.monitor.vo.OnlineUserVO;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;
import com.admin.server.modules.system.service.dept.DeptService;
import com.admin.server.modules.system.service.permission.DataScopeService;
import com.admin.server.modules.system.service.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnlineUserServiceImplTest {

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private TokenService tokenService;
    @Mock
    private UserService userService;
    @Mock
    private DeptService deptService;
    @Mock
    private DataScopeService dataScopeService;
    @Mock
    private com.admin.server.framework.config.DynamicConfigProvider dynamicConfigProvider;

    @InjectMocks
    private OnlineUserServiceImpl onlineUserService;

    @Test
    @DisplayName("张三只能看到本部门/自己在线，admin和lisi被隔离过滤")
    void testListOnlineUsers_WithDataScopeFilter() {
        try (MockedStatic<StpUtil> mockedStp = Mockito.mockStatic(StpUtil.class)) {
            mockedStp.when(() -> StpUtil.isLogin(1L)).thenReturn(true);
            mockedStp.when(() -> StpUtil.isLogin(2L)).thenReturn(true);
            mockedStp.when(() -> StpUtil.isLogin(5L)).thenReturn(true);

            // 模拟 Redis/TokenService 中有三个在线用户：1 (admin), 2 (zhangsan), 5 (lisi)
            when(tokenService.listActiveUserIds()).thenReturn(List.of(1L, 2L, 5L));

            // 模拟当前用户张三的权限范围：非超管
            DataScopeService.DataScopeContext scopeContext =
                    new DataScopeService.DataScopeContext(false, false, List.of(2L), 2L, 2L);
            when(dataScopeService.getCurrentScope()).thenReturn(scopeContext);

            // 权限判定：张三可见，admin 和 lisi 不可见
            when(dataScopeService.isUserVisible(1L)).thenReturn(false);
            when(dataScopeService.isUserVisible(2L)).thenReturn(true);
            when(dataScopeService.isUserVisible(5L)).thenReturn(false);

            // 模拟 Token 登录详情
            TokenService.LoginInfo info2 = new TokenService.LoginInfo();
            info2.setUserId(2L);
            info2.setUsername("zhangsan");
            info2.setCreateTime(System.currentTimeMillis());
            when(tokenService.getLoginInfo(2L)).thenReturn(info2);

            when(dynamicConfigProvider.getTokenExpirationMs()).thenReturn(1800000L);
            @SuppressWarnings("unchecked")
            RBucket<Object> mockBucket = mock(RBucket.class);
            when(redissonClient.getBucket(anyString())).thenReturn(mockBucket);

            UserDO user2 = new UserDO();
            user2.setId(2L);
            user2.setUsername("zhangsan");
            user2.setNickname("张三");
            user2.setDeptId(2L);
            when(userService.getUserForDisplay(2L)).thenReturn(user2);

            // 校验在线用户数量统计
            int count = onlineUserService.countOnlineUsers();
            assertThat(count).isEqualTo(1);

            // 校验在线用户列表
            List<OnlineUserVO> list = onlineUserService.listOnlineUsers();
            assertThat(list).hasSize(1);
            assertThat(list.get(0).getUserId()).isEqualTo(2L);
            assertThat(list.get(0).getLoginName()).isEqualTo("zhangsan");
        }
    }

    @Test
    @DisplayName("张三尝试强退非管辖用户admin时抛出403禁止操作")
    void testForceLogout_Forbidden() {
        DataScopeService.DataScopeContext scopeContext =
                new DataScopeService.DataScopeContext(false, false, List.of(2L), 2L, 2L);
        when(dataScopeService.getCurrentScope()).thenReturn(scopeContext);
        when(dataScopeService.isUserVisible(1L)).thenReturn(false);

        assertThatThrownBy(() -> onlineUserService.forceLogout(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作该用户会话");
    }
}

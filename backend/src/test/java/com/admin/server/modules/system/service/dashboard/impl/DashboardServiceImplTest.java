package com.admin.server.modules.system.service.dashboard.impl;

import com.admin.server.modules.infra.service.file.SysFileService;
import com.admin.server.modules.infra.service.monitor.OnlineUserService;
import com.admin.server.modules.message.service.ChatService;
import com.admin.server.modules.system.dal.dataobject.dashboard.DashboardStatsRowDO;
import com.admin.server.modules.system.dal.mysql.dashboard.DashboardMapper;
import com.admin.server.modules.system.dal.mysql.dept.DeptMapper;
import com.admin.server.modules.system.dal.mysql.loginlog.LoginLogMapper;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;
import com.admin.server.modules.system.service.config.SysConfigGroupService;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.admin.server.modules.system.service.permission.DataScopeService;
import com.admin.server.testsupport.MybatisLambdaTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DashboardServiceImpl 单元测试")
class DashboardServiceImplTest extends MybatisLambdaTestBase {

    @Mock
    private DashboardMapper dashboardMapper;
    @Mock
    private LoginLogMapper loginLogMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private DeptMapper deptMapper;
    @Mock
    private DataScopeService dataScopeService;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private OnlineUserService onlineUserService;
    @Mock
    private SystemConfigHelper systemConfigHelper;
    @Mock
    private SysConfigGroupService configGroupService;
    @Mock
    private ChatService chatService;
    @Mock
    private SysFileService sysFileService;
    @Mock
    private RBucket<String> cacheBucket;
    @Mock
    private RBucket<Long> visitBucket;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        doReturn(cacheBucket).when(redissonClient).getBucket(org.mockito.ArgumentMatchers.contains("aggregate"));
        doReturn(visitBucket).when(redissonClient).getBucket(org.mockito.ArgumentMatchers.contains("visit"));
        when(cacheBucket.get()).thenReturn(null);
        when(visitBucket.get()).thenReturn(10L);

        DashboardStatsRowDO row = new DashboardStatsRowDO();
        row.setUserCount(4L);
        row.setRoleCount(2L);
        row.setDeptCount(3L);
        row.setMenuCount(121L);
        when(dashboardMapper.selectAggregateStats(any(), any(), any())).thenReturn(row);
        when(onlineUserService.countOnlineUsers()).thenReturn(2);
        when(configGroupService.listAll()).thenReturn(List.of());
    }

    @Test
    @DisplayName("getStats：超级管理员返回全量统计数据（userCount=4, deptCount=3）")
    void getStats_superAdmin_returnsGlobalStats() {
        when(dataScopeService.getCurrentScope())
                .thenReturn(new DataScopeService.DataScopeContext(true, false, List.of(), 1L, 1L));

        Map<String, Object> stats = dashboardService.getStats(1L);

        assertEquals(4L, stats.get("userCount"));
        assertEquals(3L, stats.get("deptCount"));
    }

    @Test
    @DisplayName("getStats：普通用户按组织数据权限隔离（userCount=1, deptCount=1）")
    void getStats_scopedUser_returnsScopedStats() {
        when(dataScopeService.getCurrentScope())
                .thenReturn(new DataScopeService.DataScopeContext(false, false, List.of(2L), 2L, 2L));
        when(userMapper.selectCount(com.admin.server.testsupport.MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(1L);
        when(deptMapper.selectCount(com.admin.server.testsupport.MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(1L);

        Map<String, Object> stats = dashboardService.getStats(2L);

        assertEquals(1L, stats.get("userCount"));
        assertEquals(1L, stats.get("deptCount"));
    }
}

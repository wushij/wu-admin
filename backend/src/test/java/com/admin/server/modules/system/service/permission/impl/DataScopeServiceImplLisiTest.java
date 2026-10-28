package com.admin.server.modules.system.service.permission.impl;

import com.admin.server.modules.system.dal.dataobject.permission.RoleDO;
import com.admin.server.modules.system.dal.dataobject.permission.UserRoleDO;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;
import com.admin.server.modules.system.dal.mysql.dept.DeptMapper;
import com.admin.server.modules.system.dal.mysql.permission.RoleMapper;
import com.admin.server.modules.system.dal.mysql.permission.UserRoleMapper;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;
import com.admin.server.modules.system.service.permission.DataScopeService;
import com.admin.server.testsupport.MybatisLambdaTestBase;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.admin.server.framework.security.core.service.SecurityUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DataScopeServiceImpl 针对未分配部门用户的测试")
class DataScopeServiceImplLisiTest extends MybatisLambdaTestBase {

    @Mock
    private UserMapper userMapper;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private DeptMapper deptMapper;

    @InjectMocks
    private DataScopeServiceImpl dataScopeService;

    @Test
    @DisplayName("未分配部门且角色为本部门及以下(4)的用户，applyUserScope应限制为仅本人")
    void applyUserScope_userWithoutDept_shouldScopeToSelf() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getLoginUserId).thenReturn(5L);

            UserDO lisi = new UserDO();
            lisi.setId(5L);
            lisi.setUsername("lisi");
            lisi.setDeptId(null);
            when(userMapper.selectById(5L)).thenReturn(lisi);

            UserRoleDO userRole = new UserRoleDO();
            userRole.setUserId(5L);
            userRole.setRoleId(2L);
            when(userRoleMapper.selectListByUserId(5L)).thenReturn(List.of(userRole));

            RoleDO role = new RoleDO();
            role.setId(2L);
            role.setCode("user");
            role.setDataScope(4);
            when(roleMapper.selectByIds(anyCollection())).thenReturn(List.of(role));

            DataScopeService.DataScopeContext scope = dataScopeService.getCurrentScope();
            assertFalse(scope.all(), "未分配部门的用户不应具有全部数据范围");
            assertTrue(scope.deptIds().isEmpty(), "未分配部门的用户部门列表应为空");

            LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
            dataScopeService.applyUserScope(wrapper);

            String sqlSelect = wrapper.getCustomSqlSegment();
            System.out.println("Generated SQL Segment: " + sqlSelect);
            assertNotNull(sqlSelect);
            assertTrue(sqlSelect.contains("id = #{ew.paramNameValuePairs.MPGENVAL1}"), "应该包含 id 限制");
        }
    }
}

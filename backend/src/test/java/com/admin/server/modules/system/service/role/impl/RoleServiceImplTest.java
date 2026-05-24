package com.admin.server.modules.system.service.role.impl;

import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.system.api.role.vo.RoleCreateReqVO;
import com.admin.server.modules.system.api.role.vo.RoleUpdateReqVO;
import com.admin.server.modules.system.dal.dataobject.permission.RoleDO;
import com.admin.server.modules.system.dal.mysql.permission.RoleMapper;
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

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleServiceImpl 单元测试")
class RoleServiceImplTest extends MybatisLambdaTestBase {

    @Mock
    private RoleMapper roleMapper;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    @DisplayName("create：插入角色并返回 ID")
    void create_returnsId() {
        RoleCreateReqVO req = new RoleCreateReqVO();
        req.setName("运营");
        req.setCode("operator");
        req.setSort(10);

        doAnswer(inv -> {
            RoleDO role = inv.getArgument(0);
            role.setId(5L);
            return 1;
        }).when(roleMapper).insert(any(RoleDO.class));

        Long id = roleService.create(req);

        assertEquals(5L, id);
        verify(roleMapper).insert(argThat((RoleDO r) -> "operator".equals(r.getCode()) && r.getStatus() == 1));
    }

    @Test
    @DisplayName("update：角色不存在抛 404")
    void update_notFound() {
        RoleUpdateReqVO req = new RoleUpdateReqVO();
        req.setId(99L);
        when(roleMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> roleService.update(req));

        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("updateStatus：角色不存在抛 404")
    void updateStatus_notFound() {
        when(roleMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> roleService.updateStatus(1L, 0));

        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("list：为每个角色填充 menuIds")
    void list_fillsMenuIds() {
        RoleDO role = ServiceTestFixtures.role(1L, "admin");
        role.setName("管理员");
        when(roleMapper.selectList(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(List.of(role));
        when(permissionService.getRoleMenuListByRoleId(1L)).thenReturn(Set.of(10L, 20L));

        List<RoleDO> roles = roleService.list(null, null);

        assertEquals(1, roles.size());
        assertEquals(Set.of(10L, 20L), roles.get(0).getMenuIds());
    }

    @Test
    @DisplayName("assignMenu：委托 PermissionService 分配菜单")
    void assignMenu_delegates() {
        roleService.assignMenu(3L, Set.of(1L, 2L));
        verify(permissionService).assignRoleMenu(3L, Set.of(1L, 2L));
    }

    @Test
    @DisplayName("restore：回收站角色不存在抛 404")
    void restore_notFound() {
        when(roleMapper.restoreById(7L)).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> roleService.restore(7L));

        assertEquals(404, ex.getCode());
    }
}

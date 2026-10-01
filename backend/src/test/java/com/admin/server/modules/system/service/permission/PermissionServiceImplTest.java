package com.admin.server.modules.system.service.permission;

import com.admin.server.modules.system.dal.dataobject.permission.MenuDO;
import com.admin.server.modules.system.dal.dataobject.permission.RoleDO;
import com.admin.server.modules.system.dal.mysql.permission.MenuMapper;
import com.admin.server.modules.system.dal.mysql.permission.RoleMapper;
import com.admin.server.modules.system.dal.mysql.permission.RoleMenuMapper;
import com.admin.server.modules.system.dal.mysql.permission.UserRoleMapper;
import com.admin.server.testsupport.MybatisLambdaTestBase;
import com.admin.server.testsupport.MybatisMockMatchers;
import com.admin.server.testsupport.ServiceTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PermissionServiceImpl 单元测试")
class PermissionServiceImplTest extends MybatisLambdaTestBase {

    private static final long USER_ID = 100L;
    private static final long ROLE_ID = 1L;

    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private RoleMenuMapper roleMenuMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private MenuMapper menuMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    @BeforeEach
    void stubUserRoleAndRoleMenu() {
        when(userRoleMapper.selectListByUserId(USER_ID))
                .thenReturn(List.of(ServiceTestFixtures.userRole(USER_ID, ROLE_ID)));
        when(roleMenuMapper.selectListByRoleId(ROLE_ID))
                .thenReturn(List.of(ServiceTestFixtures.roleMenu(ROLE_ID, 10L)));
    }

    @Test
    @DisplayName("hasPermission：直接匹配权限码")
    void hasPermission_directMatch() {
        MenuDO menu = ServiceTestFixtures.menu(10L, 0L, 1, "system:user:list");
        when(menuMapper.selectList(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(List.of(menu));

        assertTrue(permissionService.hasPermission(USER_ID, "system:user:list"));
    }

    @Test
    @DisplayName("hasPermission：仅有 query 权限时可访问 list 接口")
    void hasPermission_queryAliasAllowsList() {
        MenuDO menu = ServiceTestFixtures.menu(10L, 0L, 1, "system:user:query");
        when(menuMapper.selectList(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(List.of(menu));

        assertTrue(permissionService.hasPermission(USER_ID, "system:user:list"));
    }

    @Test
    @DisplayName("hasPermission：空权限码返回 false")
    void hasPermission_blankReturnsFalse() {
        assertFalse(permissionService.hasPermission(USER_ID, ""));
        assertFalse(permissionService.hasPermission(USER_ID, null));
    }

    @Test
    @DisplayName("hasRole：匹配角色编码")
    void hasRole_matchesCode() {
        RoleDO role = ServiceTestFixtures.role(ROLE_ID, "super_admin");
        when(roleMapper.selectByIds(Set.of(ROLE_ID))).thenReturn(List.of(role));

        assertTrue(permissionService.hasRole(USER_ID, "super_admin"));
        assertFalse(permissionService.hasRole(USER_ID, "normal"));
    }

    @Test
    @DisplayName("getUserMenuList：父级禁用时子菜单不可见")
    void getUserMenuList_hidesChildWhenAncestorDisabled() {
        MenuDO disabledParent = ServiceTestFixtures.menu(1L, 0L, 0, null);
        MenuDO child = ServiceTestFixtures.menu(10L, 1L, 1, "system:demo:list");
        List<MenuDO> allMenus = List.of(disabledParent, child);

        when(menuMapper.selectList(isNull())).thenReturn(allMenus);
        when(menuMapper.selectList(MybatisMockMatchers.anyLambdaQueryWrapper()))
                .thenReturn(allMenus)
                .thenReturn(List.of(child));

        List<MenuDO> menus = permissionService.getUserMenuList(USER_ID);

        assertTrue(menus.isEmpty());
    }

    @Test
    @DisplayName("getUserMenuList：无角色时返回空列表")
    void getUserMenuList_noRolesReturnsEmpty() {
        when(userRoleMapper.selectListByUserId(999L)).thenReturn(List.of());

        assertTrue(permissionService.getUserMenuList(999L).isEmpty());
    }

    @Test
    @DisplayName("assignUserRole：先删后增角色关联")
    void assignUserRole_replacesLinks() {
        permissionService.assignUserRole(USER_ID, Set.of(2L, 3L));

        verify(userRoleMapper).deleteByUserId(USER_ID);
        verify(userRoleMapper).insertBatch(argThat(list -> list != null && list.size() == 2));
    }

    @Test
    @DisplayName("assignUserRole：空角色集仅删除旧关联")
    void assignUserRole_clearsWhenEmpty() {
        permissionService.assignUserRole(USER_ID, Set.of());

        verify(userRoleMapper).deleteByUserId(USER_ID);
        verify(userRoleMapper, never()).insertBatch(any());
    }

    @Test
    @DisplayName("hasPermission：仅 AI 查询按钮时可访问 list 接口")
    void hasPermission_aiQueryAllowsList() {
        MenuDO dir = ServiceTestFixtures.menu(210L, 0L, 1, null);
        MenuDO page = ServiceTestFixtures.menu(200L, 210L, 2, "system:ai-model:list");
        MenuDO queryBtn = ServiceTestFixtures.menu(207L, 200L, 3, "system:ai-model:query");
        when(roleMenuMapper.selectListByRoleId(ROLE_ID))
                .thenReturn(List.of(ServiceTestFixtures.roleMenu(ROLE_ID, 207L)));
        when(menuMapper.selectList(isNull())).thenReturn(List.of(dir, page, queryBtn));
        when(menuMapper.selectList(MybatisMockMatchers.anyLambdaQueryWrapper()))
                .thenReturn(List.of(dir, page, queryBtn));

        assertTrue(permissionService.hasPermission(USER_ID, "system:ai-model:list"));
        assertTrue(permissionService.hasPermission(USER_ID, "system:ai-model:query"));
        assertFalse(permissionService.hasPermission(USER_ID, "system:ai-model:create"));
    }

    @Test
    @DisplayName("getUserPermissionCodes：query 权限自动附带 list 别名")
    void getUserPermissionCodes_queryAddsListAlias() {
        MenuDO page = ServiceTestFixtures.menu(205L, 210L, 2, "system:ai-log:list");
        MenuDO queryBtn = ServiceTestFixtures.menu(208L, 205L, 3, "system:ai-log:query");
        when(roleMenuMapper.selectListByRoleId(ROLE_ID))
                .thenReturn(List.of(ServiceTestFixtures.roleMenu(ROLE_ID, 208L)));
        when(menuMapper.selectList(isNull())).thenReturn(List.of(page, queryBtn));
        when(menuMapper.selectList(MybatisMockMatchers.anyLambdaQueryWrapper()))
                .thenReturn(List.of(page, queryBtn));

        Set<String> codes = permissionService.getUserPermissionCodes(USER_ID);

        assertTrue(codes.contains("system:ai-log:query"));
        assertTrue(codes.contains("system:ai-log:list"));
    }

    @Test
    @DisplayName("hasPermission：仅勾选按钮时继承父级 list 权限（与侧栏一致）")
    void hasPermission_buttonInheritsParentList() {
        MenuDO dir = ServiceTestFixtures.menu(210L, 0L, 1, null);
        MenuDO page = ServiceTestFixtures.menu(200L, 210L, 2, "system:ai-model:list");
        MenuDO createBtn = ServiceTestFixtures.menu(201L, 200L, 3, "system:ai-model:create");
        when(roleMenuMapper.selectListByRoleId(ROLE_ID))
                .thenReturn(List.of(ServiceTestFixtures.roleMenu(ROLE_ID, 201L)));
        when(menuMapper.selectList(isNull())).thenReturn(List.of(dir, page, createBtn));
        when(menuMapper.selectList(MybatisMockMatchers.anyLambdaQueryWrapper()))
                .thenReturn(List.of(dir, page, createBtn));

        assertTrue(permissionService.hasPermission(USER_ID, "system:ai-model:list"));
        assertTrue(permissionService.hasPermission(USER_ID, "system:ai-model:create"));
        assertFalse(permissionService.hasPermission(USER_ID, "system:ai-model:delete"));
    }

    @Test
    @DisplayName("assignRoleMenu：保存时自动补齐父级菜单")
    void assignRoleMenu_expandsParentMenus() {
        MenuDO dir = ServiceTestFixtures.menu(210L, 0L, 1, null);
        MenuDO page = ServiceTestFixtures.menu(200L, 210L, 2, "system:ai-model:list");
        MenuDO createBtn = ServiceTestFixtures.menu(201L, 200L, 3, "system:ai-model:create");
        when(menuMapper.selectList(isNull())).thenReturn(List.of(dir, page, createBtn));

        permissionService.assignRoleMenu(ROLE_ID, Set.of(201L));

        verify(roleMenuMapper).insertBatch(argThat(list ->
                list != null && list.size() == 3
                        && list.stream().anyMatch(rm -> rm.getMenuId().equals(210L))
                        && list.stream().anyMatch(rm -> rm.getMenuId().equals(200L))
                        && list.stream().anyMatch(rm -> rm.getMenuId().equals(201L))));
    }

    @Test
    @DisplayName("assignRoleMenu：先删后增菜单关联")
    void assignRoleMenu_replacesLinks() {
        permissionService.assignRoleMenu(ROLE_ID, Set.of(10L, 20L));

        verify(roleMenuMapper).deleteByRoleId(ROLE_ID);
        verify(roleMenuMapper).insertBatch(argThat(list -> list != null && list.size() == 2));
    }
}

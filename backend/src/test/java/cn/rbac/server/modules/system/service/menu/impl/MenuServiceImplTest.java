package cn.rbac.server.modules.system.service.menu.impl;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.modules.system.api.menu.vo.MenuCreateReqVO;
import cn.rbac.server.modules.system.api.menu.vo.MenuUpdateReqVO;
import cn.rbac.server.modules.system.dal.dataobject.permission.MenuDO;
import cn.rbac.server.modules.system.dal.mysql.permission.MenuMapper;
import cn.rbac.server.testsupport.MybatisLambdaTestBase;
import cn.rbac.server.testsupport.MybatisMockMatchers;
import cn.rbac.server.testsupport.ServiceTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MenuServiceImpl 单元测试")
class MenuServiceImplTest extends MybatisLambdaTestBase {

    @Mock
    private MenuMapper menuMapper;

    @InjectMocks
    private MenuServiceImpl menuService;

    @Test
    @DisplayName("listTree：父级禁用时子级不展示")
    void listTree_hidesChildWhenParentDisabled() {
        MenuDO parent = ServiceTestFixtures.menu(1L, 0L, 0, null);
        parent.setName("系统");
        parent.setType(1);
        MenuDO child = ServiceTestFixtures.menu(10L, 1L, 1, "system:user:list");
        child.setName("用户管理");
        child.setType(2);

        when(menuMapper.selectList(MybatisMockMatchers.anyLambdaQueryWrapper()))
                .thenReturn(List.of(parent, child));

        List<MenuDO> tree = menuService.listTree(null, null, null);
        List<Long> ids = collectMenuIds(tree);

        assertFalse(ids.contains(10L), "禁用父级下的子菜单不应出现在树中");
        if (!ids.isEmpty()) {
            MenuDO root = findMenu(tree, 1L);
            if (root != null) {
                assertTrue(root.getChildren() == null || root.getChildren().isEmpty());
            }
        }
    }

    @Test
    @DisplayName("deleteMenu：存在子菜单时拒绝删除")
    void deleteMenu_rejectsWhenHasChildren() {
        when(menuMapper.selectCount(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> menuService.deleteMenu(1L));

        assertTrue(ex.getMessage().contains("子菜单"));
        verify(menuMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("deleteMenu：无子菜单时可删除")
    void deleteMenu_succeedsWhenNoChildren() {
        when(menuMapper.selectCount(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(0L);

        menuService.deleteMenu(1L);

        verify(menuMapper).deleteById(eq(1L));
    }

    @Test
    @DisplayName("createMenu：插入并返回 ID")
    void createMenu_insertsAndReturnsId() {
        MenuCreateReqVO req = new MenuCreateReqVO();
        req.setName("新菜单");
        req.setType(2);
        req.setPath("demo");

        doAnswer(inv -> {
            MenuDO menu = inv.getArgument(0);
            menu.setId(100L);
            return 1;
        }).when(menuMapper).insert(any(MenuDO.class));

        Long id = menuService.createMenu(req);

        assertEquals(100L, id);
        verify(menuMapper).insert(argThat((MenuDO m) ->
                "新菜单".equals(m.getName()) && m.getStatus() == 1 && m.getParentId() == 0L));
    }

    @Test
    @DisplayName("updateMenu：菜单不存在抛 404")
    void updateMenu_notFound() {
        when(menuMapper.selectById(1L)).thenReturn(null);
        MenuUpdateReqVO req = new MenuUpdateReqVO();
        req.setId(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> menuService.updateMenu(req));

        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("updateStatus：更新菜单状态")
    void updateStatus_updatesMenu() {
        MenuDO menu = ServiceTestFixtures.menu(1L, 0L, 1, null);
        when(menuMapper.selectById(1L)).thenReturn(menu);

        menuService.updateStatus(1L, 0);

        verify(menuMapper).updateById(argThat((MenuDO m) -> m.getStatus() == 0));
    }

    @Test
    @DisplayName("restoreMenu：回收站不存在抛 404")
    void restoreMenu_notFound() {
        when(menuMapper.restoreById(1L)).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> menuService.restoreMenu(1L));

        assertEquals(404, ex.getCode());
    }

    private static List<Long> collectMenuIds(List<MenuDO> nodes) {
        List<Long> ids = new ArrayList<>();
        if (nodes == null) {
            return ids;
        }
        for (MenuDO node : nodes) {
            ids.add(node.getId());
            if (node.getChildren() != null) {
                ids.addAll(collectMenuIds(node.getChildren()));
            }
        }
        return ids;
    }

    private static MenuDO findMenu(List<MenuDO> nodes, long id) {
        if (nodes == null) {
            return null;
        }
        for (MenuDO node : nodes) {
            if (node.getId() == id) {
                return node;
            }
            MenuDO found = findMenu(node.getChildren(), id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}

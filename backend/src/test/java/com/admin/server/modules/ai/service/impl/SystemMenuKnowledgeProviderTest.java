package com.admin.server.modules.ai.service.impl;

import com.admin.server.modules.system.dal.dataobject.permission.MenuDO;
import com.admin.server.modules.system.service.permission.PermissionService;
import com.admin.server.testsupport.ServiceTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SystemMenuKnowledgeProvider 单元测试")
class SystemMenuKnowledgeProviderTest {

    private static final long USER_ID = 100L;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private SystemMenuKnowledgeProvider provider;

    @Test
    @DisplayName("buildMenuKnowledge：顶级带二级明细，过滤按钮(type3)与禁用项")
    void buildMenuKnowledge_topWithChildren() {
        MenuDO top = ServiceTestFixtures.menu(1L, 0L, 1, 1, "系统管理");
        MenuDO user = ServiceTestFixtures.menu(2L, 1L, 1, 2, "用户管理");
        MenuDO role = ServiceTestFixtures.menu(3L, 1L, 1, 2, "角色管理");
        MenuDO button = ServiceTestFixtures.menu(4L, 2L, 1, 3, "新增用户"); // 按钮应被过滤
        MenuDO disabled = ServiceTestFixtures.menu(5L, 1L, 0, 2, "禁用菜单"); // 禁用应被过滤
        when(permissionService.getUserMenuList(USER_ID))
                .thenReturn(List.of(top, user, role, button, disabled));

        String result = provider.buildMenuKnowledge(USER_ID);

        assertTrue(result.contains("系统管理"));
        assertTrue(result.contains("用户管理"));
        assertTrue(result.contains("角色管理"));
        assertFalse(result.contains("新增用户"), "按钮不应出现在菜单知识中");
        assertFalse(result.contains("禁用菜单"), "禁用菜单不应出现");
    }

    @Test
    @DisplayName("buildTopLevelOnly：仅输出顶级菜单名")
    void buildTopLevelOnly_onlyTops() {
        MenuDO top1 = ServiceTestFixtures.menu(1L, 0L, 1, 1, "系统管理");
        MenuDO top2 = ServiceTestFixtures.menu(10L, 0L, 1, 1, "监控中心");
        MenuDO child = ServiceTestFixtures.menu(2L, 1L, 1, 2, "用户管理");
        when(permissionService.getUserMenuList(USER_ID)).thenReturn(List.of(top1, top2, child));

        String result = provider.buildTopLevelOnly(USER_ID);

        assertTrue(result.contains("系统管理"));
        assertTrue(result.contains("监控中心"));
        assertFalse(result.contains("用户管理"), "顶级降级版不应含二级菜单");
    }

    @Test
    @DisplayName("空/无效 userId 返回空串，不触发查询")
    void invalidUser_returnsEmpty() {
        assertEquals("", provider.buildMenuKnowledge(0L));
        assertEquals("", provider.buildMenuKnowledge(null));
        assertEquals("", provider.buildTopLevelOnly(-1L));
    }

    @Test
    @DisplayName("无可见菜单返回空串")
    void noVisibleMenu_returnsEmpty() {
        when(permissionService.getUserMenuList(USER_ID)).thenReturn(List.of());
        assertEquals("", provider.buildMenuKnowledge(USER_ID));
    }
}

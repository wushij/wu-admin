package com.admin.server.modules.ai.service.impl;

import com.admin.server.modules.system.dal.dataobject.dept.DeptDO;
import com.admin.server.modules.system.dal.dataobject.permission.RoleDO;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.admin.server.modules.system.service.dept.DeptService;
import com.admin.server.modules.system.service.permission.PermissionService;
import com.admin.server.modules.system.service.permission.event.UserPermissionChangedEvent;
import com.admin.server.modules.system.service.role.RoleService;
import com.admin.server.modules.system.service.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromptAssembler 单元测试")
class PromptAssemblerTest {

    private static final long USER_ID = 100L;

    @Mock
    private SystemConfigHelper systemConfigHelper;
    @Mock
    private SystemMenuKnowledgeProvider menuKnowledgeProvider;
    @Mock
    private UserService userService;
    @Mock
    private DeptService deptService;
    @Mock
    private PermissionService permissionService;
    @Mock
    private RoleService roleService;
    @Mock
    private KnowledgeRetriever knowledgeRetriever;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PromptAssembler promptAssembler;

    @Test
    @DisplayName("assemble：完整三段组装，包含人设、全局知识、菜单、用户上下文、知识库与防注入")
    void assemble_full() {
        when(systemConfigHelper.getAiAnswerScope()).thenReturn("focus");
        when(systemConfigHelper.getAiGlobalKnowledge()).thenReturn("- 系统名称：Admin Platform");
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        UserDO user = new UserDO();
        user.setUsername("zhangsan");
        user.setNickname("张三");
        user.setDeptId(10L);
        when(userService.getDetail(USER_ID)).thenReturn(user);

        DeptDO dept = new DeptDO();
        dept.setName("技术部");
        when(deptService.getById(10L)).thenReturn(dept);

        when(permissionService.getUserRoleIdListByUserId(USER_ID)).thenReturn(Set.of(1L));
        RoleDO role = new RoleDO();
        role.setName("管理员");
        when(roleService.getById(1L)).thenReturn(role);

        when(menuKnowledgeProvider.buildMenuKnowledge(USER_ID)).thenReturn("- 系统管理(用户/角色)");
        when(menuKnowledgeProvider.buildTopLevelOnly(USER_ID)).thenReturn("- 系统管理");

        when(knowledgeRetriever.retrieveReferenceBlock("重置密码")).thenReturn("<reference_data>重置密码步骤</reference_data>");

        String result = promptAssembler.assemble(USER_ID, "请保持礼貌", "pc", "重置密码");

        assertTrue(result.contains("AI wu助手"));
        assertTrue(result.contains("<project_knowledge>"));
        assertTrue(result.contains("Admin Platform"));
        assertTrue(result.contains("<menu_structure>"));
        assertTrue(result.contains("<user_context>"));
        assertTrue(result.contains("张三"));
        assertTrue(result.contains("技术部"));
        assertTrue(result.contains("管理员"));
        assertTrue(result.contains("<reference_data>"));
        assertTrue(result.contains("请保持礼貌"));
        assertTrue(result.contains("安全声明"));
    }

    @Test
    @DisplayName("onUserPermissionChanged：处理权限变更事件，清理 Redis 缓存")
    void onUserPermissionChanged_evictsCache() {
        UserPermissionChangedEvent event = new UserPermissionChangedEvent(USER_ID);
        promptAssembler.onUserPermissionChanged(event);
        verify(stringRedisTemplate).delete("ai:user_context:" + USER_ID);
    }
}

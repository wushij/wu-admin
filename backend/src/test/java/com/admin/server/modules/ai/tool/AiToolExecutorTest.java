package com.admin.server.modules.ai.tool;

import cn.hutool.json.JSONObject;
import com.admin.server.modules.system.service.permission.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AiToolExecutor 单元测试")
class AiToolExecutorTest {

    private static final long ADMIN_USER = 1L;
    private static final long NORMAL_USER = 2L;

    @Mock
    private PermissionService permissionService;

    private AiToolExecutor executor;

    /** USER 级工具：回显上下文 userId，验证身份从会话注入 */
    private final AiTool userTool = new StubTool("get_my_todo_count", ToolAccessLevel.USER, null,
            (args, ctx) -> new JSONObject().set("userId", ctx.getUserId()).toString());

    /** ADMIN 级工具 */
    private final AiTool adminTool = new StubTool("get_online_user_count", ToolAccessLevel.ADMIN, "monitor:online:list",
            (args, ctx) -> new JSONObject().set("onlineUserCount", 42).toString());

    @BeforeEach
    void setup() {
        executor = new AiToolExecutor();
        ReflectionTestUtils.setField(executor, "tools", List.of(userTool, adminTool));
        ReflectionTestUtils.setField(executor, "permissionService", permissionService);
        ReflectionTestUtils.setField(executor, "toolTimeoutMs", 3000L);
        executor.init();
    }

    @Test
    @DisplayName("可见性过滤：普通用户仅见 USER 级工具，不含 ADMIN 工具声明")
    void buildToolSpecs_normalUserExcludesAdminTool() {
        when(permissionService.hasRole(NORMAL_USER, "super_admin")).thenReturn(false);
        when(permissionService.hasPermission(NORMAL_USER, "monitor:online:list")).thenReturn(false);

        String specs = executor.buildToolSpecs(NORMAL_USER);

        assertNotNull(specs);
        assertTrue(specs.contains("get_my_todo_count"));
        assertFalse(specs.contains("get_online_user_count"), "普通用户请求体不应出现 ADMIN 工具");
    }

    @Test
    @DisplayName("可见性过滤：超管可见全部工具")
    void buildToolSpecs_superAdminSeesAll() {
        when(permissionService.hasRole(ADMIN_USER, "super_admin")).thenReturn(true);

        String specs = executor.buildToolSpecs(ADMIN_USER);

        assertTrue(specs.contains("get_my_todo_count"));
        assertTrue(specs.contains("get_online_user_count"));
    }

    @Test
    @DisplayName("二次鉴权：普通用户调用 ADMIN 工具被拒绝，返回话术不抛异常")
    void invoke_normalUserDeniedAdminTool() {
        when(permissionService.hasRole(NORMAL_USER, "super_admin")).thenReturn(false);
        when(permissionService.hasPermission(NORMAL_USER, "monitor:online:list")).thenReturn(false);

        ToolInvocationResult r = executor.invoke("get_online_user_count", "{}", new AiToolContext(NORMAL_USER, "bob"));

        assertTrue(r.isDenied());
        assertTrue(r.getResult().contains("无权"));
    }

    @Test
    @DisplayName("未注册工具被拒绝")
    void invoke_unknownToolDenied() {
        ToolInvocationResult r = executor.invoke("drop_table", "{}", new AiToolContext(ADMIN_USER, "admin"));
        assertTrue(r.isDenied());
    }

    @Test
    @DisplayName("USER 级工具执行成功，且 userId 强制来自会话上下文")
    void invoke_userToolInjectsSessionUserId() {
        ToolInvocationResult r = executor.invoke("get_my_todo_count", "{\"userId\":999}",
                new AiToolContext(NORMAL_USER, "bob"));

        assertFalse(r.isDenied());
        assertTrue(r.getResult().contains("\"userId\":2"), "应使用会话 userId=2 而非模型传入的 999");
    }

    /** 测试桩工具 */
    private static class StubTool implements AiTool {
        private final String name;
        private final ToolAccessLevel level;
        private final String permission;
        private final java.util.function.BiFunction<JSONObject, AiToolContext, String> fn;

        StubTool(String name, ToolAccessLevel level, String permission,
                 java.util.function.BiFunction<JSONObject, AiToolContext, String> fn) {
            this.name = name;
            this.level = level;
            this.permission = permission;
            this.fn = fn;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return name;
        }

        @Override
        public JSONObject parametersSchema() {
            return new JSONObject().set("type", "object").set("properties", new JSONObject());
        }

        @Override
        public ToolAccessLevel accessLevel() {
            return level;
        }

        @Override
        public String requiredPermission() {
            return permission;
        }

        @Override
        public String execute(JSONObject args, AiToolContext ctx) {
            return fn.apply(args, ctx);
        }
    }
}

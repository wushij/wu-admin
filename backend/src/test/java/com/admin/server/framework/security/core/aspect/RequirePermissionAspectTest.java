package com.admin.server.framework.security.core.aspect;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.admin.server.framework.security.api.PermissionApi;
import com.admin.server.framework.security.api.PermissionDeniedException;
import com.admin.server.framework.security.api.RequirePermission;
import com.admin.server.framework.security.core.RiskScoringApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 校验 {@link RequirePermissionAspect} 与改造前 {@code @PreAuthorize("@ss.hasXxx(...)")} 逐项等价。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RequirePermissionAspect 单元测试")
class RequirePermissionAspectTest {

    @Mock
    private PermissionApi permissionApi;

    @Mock
    private RiskScoringApi antiBotRiskService;

    @Mock
    private RequirePermission annotation;

    private RequirePermissionAspect aspect;
    private MockedStatic<StpUtil> stpUtil;

    @BeforeEach
    void setUp() {
        aspect = new RequirePermissionAspect(permissionApi, antiBotRiskService);
        stpUtil = Mockito.mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        stpUtil.close();
    }

    @Test
    @DisplayName("PERMISSION：有权限放行")
    void permission_granted() {
        when(annotation.type()).thenReturn(RequirePermission.Type.PERMISSION);
        when(annotation.value()).thenReturn("system:user:create");
        when(permissionApi.hasPermission("system:user:create")).thenReturn(true);

        assertDoesNotThrow(() -> aspect.checkMethod(annotation));
    }

    @Test
    @DisplayName("PERMISSION：无权限抛 PermissionDeniedException")
    void permission_denied() {
        when(annotation.type()).thenReturn(RequirePermission.Type.PERMISSION);
        when(annotation.value()).thenReturn("system:user:create");
        when(permissionApi.hasPermission("system:user:create")).thenReturn(false);

        PermissionDeniedException ex = assertThrows(PermissionDeniedException.class,
                () -> aspect.checkMethod(annotation));
        assertEquals("权限不足，无法访问", ex.getMessage());
    }

    @Test
    @DisplayName("READ：走 hasRead 的 list/query/upload 兼容逻辑")
    void read_usesHasRead() {
        when(annotation.type()).thenReturn(RequirePermission.Type.READ);
        when(annotation.value()).thenReturn("system:user:list");
        when(permissionApi.hasRead("system:user:list")).thenReturn(true);

        assertDoesNotThrow(() -> aspect.checkMethod(annotation));
    }

    @Test
    @DisplayName("RECYCLE_READ：走 hasRecycleRead")
    void recycleRead_usesHasRecycleRead() {
        when(annotation.type()).thenReturn(RequirePermission.Type.RECYCLE_READ);
        when(permissionApi.hasRecycleRead()).thenReturn(true);

        assertDoesNotThrow(() -> aspect.checkMethod(annotation));
    }

    @Test
    @DisplayName("RECYCLE_RESTORE：走 hasRecycleRestore")
    void recycleRestore_usesHasRecycleRestore() {
        when(annotation.type()).thenReturn(RequirePermission.Type.RECYCLE_RESTORE);
        when(annotation.value()).thenReturn("system:user:delete");
        when(permissionApi.hasRecycleRestore("system:user:delete")).thenReturn(true);

        assertDoesNotThrow(() -> aspect.checkMethod(annotation));
    }

    @Test
    @DisplayName("RECYCLE_DELETE：走 hasRecycleDelete")
    void recycleDelete_usesHasRecycleDelete() {
        when(annotation.type()).thenReturn(RequirePermission.Type.RECYCLE_DELETE);
        when(annotation.value()).thenReturn("system:user:delete");
        when(permissionApi.hasRecycleDelete("system:user:delete")).thenReturn(true);

        assertDoesNotThrow(() -> aspect.checkMethod(annotation));
    }

    @Test
    @DisplayName("ROLE：走 hasRole")
    void role_usesHasRole() {
        when(annotation.type()).thenReturn(RequirePermission.Type.ROLE);
        when(annotation.value()).thenReturn("super_admin");
        when(permissionApi.hasRole("super_admin")).thenReturn(true);

        assertDoesNotThrow(() -> aspect.checkMethod(annotation));
    }

    @Test
    @DisplayName("LOGIN：仅校验登录态，不查权限码")
    void login_onlyChecksLogin() {
        when(annotation.type()).thenReturn(RequirePermission.Type.LOGIN);

        assertDoesNotThrow(() -> aspect.checkMethod(annotation));
    }

    @Test
    @DisplayName("未登录：抛 NotLoginException（由全局异常处理器转 401）")
    void notLogin_throwsNotLoginException() {
        stpUtil.when(StpUtil::checkLogin)
                .thenThrow(new NotLoginException("token", "未登录", null));

        assertThrows(NotLoginException.class, () -> aspect.checkMethod(annotation));
    }

    @Test
    @DisplayName("切点织入：@RequirePermission 方法被切面拦截并放行")
    void aspectWeavesOnAnnotatedMethod() {
        when(permissionApi.hasRead("system:user:list")).thenReturn(true);
        SampleController proxy = proxyOfSampleController();

        assertEquals("ok", proxy.list());
        verify(permissionApi).hasRead("system:user:list");
    }

    @Test
    @DisplayName("切点织入：无权限时拦截并抛 403 异常")
    void aspectBlocksWhenDenied() {
        when(permissionApi.hasRead("system:user:list")).thenReturn(false);
        SampleController proxy = proxyOfSampleController();

        assertThrows(PermissionDeniedException.class, proxy::list);
    }

    private SampleController proxyOfSampleController() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new SampleController());
        factory.addAspect(new RequirePermissionAspect(permissionApi, antiBotRiskService));
        return factory.getProxy();
    }

    /** 用于验证切点表达式的样例 Controller */
    static class SampleController {

        @RequirePermission(value = "system:user:list", type = RequirePermission.Type.READ)
        public String list() {
            return "ok";
        }
    }
}

package cn.rbac.server.framework.web.core;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.framework.config.DynamicConfigProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler 单元测试")
class GlobalExceptionHandlerTest {

    @Mock
    private DynamicConfigProvider dynamicConfigProvider;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(Objects.requireNonNull(handler),
                "dynamicConfigProvider", dynamicConfigProvider);
    }

    @Test
    @DisplayName("handleBusiness：返回业务错误码与消息")
    void handleBusiness_returnsCodeAndMessage() {
        CommonResult<Void> result = handler.handleBusiness(new BusinessException(403, "无权限"));

        assertEquals(403, result.getCode());
        assertEquals("无权限", result.getMessage());
    }

    @Test
    @DisplayName("handleIllegalArgument：返回 400")
    void handleIllegalArgument_returns400() {
        CommonResult<Void> result = handler.handleIllegalArgument(new IllegalArgumentException("参数错误"));

        assertEquals(400, result.getCode());
        assertEquals("参数错误", result.getMessage());
    }

    @Test
    @DisplayName("handleAccessDenied：返回 403")
    void handleAccessDenied_returns403() {
        CommonResult<Void> result = handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertEquals(403, result.getCode());
        assertEquals("权限不足，无法访问", result.getMessage());
    }

    @Test
    @DisplayName("handleIllegalState：返回 400")
    void handleIllegalState_returns400() {
        CommonResult<Void> result = handler.handleIllegalState(new IllegalStateException("不在该群"));

        assertEquals(400, result.getCode());
        assertEquals("不在该群", result.getMessage());
    }
}

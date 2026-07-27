package com.admin.server.framework.web.core;

import com.admin.server.common.exception.BusinessException;
import com.admin.server.common.core.CommonResult;
import com.admin.server.framework.config.DynamicConfigProvider;
import cn.dev33.satoken.exception.NotLoginException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler 单元测试")
class GlobalExceptionHandlerTest {

    @Mock
    private DynamicConfigProvider dynamicConfigProvider;

    @Mock
    private HttpServletRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(Objects.requireNonNull(handler),
                "dynamicConfigProvider", dynamicConfigProvider);
        lenient().when(request.getMethod()).thenReturn("GET");
        lenient().when(request.getRequestURI()).thenReturn("/api/test");
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    @DisplayName("handleBusiness：403 对齐 HTTP FORBIDDEN")
    void handleBusiness_mapsHttpStatus() {
        ResponseEntity<CommonResult<Void>> response =
                handler.handleBusiness(new BusinessException(403, "无权限"), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        CommonResult<Void> body = requireBody(response);
        assertEquals(403, body.getCode());
        assertEquals("无权限", body.getMessage());
    }

    @Test
    @DisplayName("handleBusiness：429 对齐 HTTP TOO_MANY_REQUESTS")
    void handleBusiness_maps429() {
        ResponseEntity<CommonResult<Void>> response =
                handler.handleBusiness(new BusinessException(429, "操作过于频繁"), request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals(429, requireBody(response).getCode());
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
    @DisplayName("handleValidation：拼接字段错误")
    void handleValidation_joinsFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "req");
        bindingResult.addError(new FieldError("req", "username", "不能为空"));
        MethodArgumentNotValidException ex = validationException(bindingResult);

        CommonResult<Void> result = handler.handleValidation(ex);

        assertTrue(result.getMessage().contains("username"));
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("handleConstraintViolation：拼接约束错误")
    void handleConstraintViolation_joinsViolations() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(violation.getPropertyPath().toString()).thenReturn("phone");
        when(violation.getMessage()).thenReturn("格式不正确");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        CommonResult<Void> result = handler.handleConstraintViolation(ex);

        assertEquals(400, result.getCode());
        assertTrue(result.getMessage().contains("phone"));
    }

    @Test
    @DisplayName("handleMissingParam：返回缺失参数名")
    void handleMissingParam_returnsParamName() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("id", "Long");

        CommonResult<Void> result = handler.handleMissingParam(ex);

        assertTrue(result.getMessage().contains("id"));
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("handleMaxUploadSize：读取动态文件大小限制")
    void handleMaxUploadSize_usesDynamicLimit() {
        when(dynamicConfigProvider.getFileMaxSizeMb()).thenReturn(50);

        CommonResult<Void> result = handler.handleMaxUploadSize(
                new org.springframework.web.multipart.MaxUploadSizeExceededException(1024L));

        assertEquals(400, result.getCode());
        assertTrue(result.getMessage().contains("50MB"));
    }

    @Test
    @DisplayName("handleValidation：多条错误用分号拼接")
    void handleValidation_joinsMultipleErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "req");
        bindingResult.addError(new FieldError("req", "username", "不能为空"));
        bindingResult.addError(new FieldError("req", "password", "长度不足"));
        MethodArgumentNotValidException ex = validationException(bindingResult);

        CommonResult<Void> result = handler.handleValidation(ex);

        assertTrue(result.getMessage().contains("username"));
        assertTrue(result.getMessage().contains("password"));
        assertTrue(result.getMessage().contains(";"));
    }

    @Test
    @DisplayName("handleValidation：无字段错误时使用默认消息")
    void handleValidation_emptyFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "req");
        MethodArgumentNotValidException ex = validationException(bindingResult);

        CommonResult<Void> result = handler.handleValidation(ex);

        assertEquals("参数验证失败", result.getMessage());
    }

    @Test
    @DisplayName("handleTypeMismatch：返回参数名")
    void handleTypeMismatch_returnsParamName() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("userId");

        CommonResult<Void> result = handler.handleTypeMismatch(ex);

        assertTrue(result.getMessage().contains("userId"));
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("handleHttpMessageNotReadable：返回 JSON 格式提示")
    void handleHttpMessageNotReadable_returnsFriendlyMessage() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMessage()).thenReturn("bad json");

        CommonResult<Void> result = handler.handleHttpMessageNotReadable(ex);

        assertEquals(400, result.getCode());
        assertTrue(result.getMessage().contains("JSON"));
    }

    @Test
    @DisplayName("handleNoHandler：返回 404")
    void handleNoHandler_returns404() {
        CommonResult<Void> result = handler.handleNoHandler(
                new NoHandlerFoundException("GET", "/missing", Objects.requireNonNull(HttpHeaders.EMPTY)));

        assertEquals(404, result.getCode());
    }

    @Test
    @DisplayName("handleMethodNotSupported：返回 405")
    void handleMethodNotSupported_returns405() {
        CommonResult<Void> result = handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("DELETE"));

        assertEquals(405, result.getCode());
        assertTrue(result.getMessage().contains("DELETE"));
    }

    @Test
    @DisplayName("handleMediaTypeNotSupported：返回 415")
    void handleMediaTypeNotSupported_returns415() {
        CommonResult<Void> result = handler.handleMediaTypeNotSupported(
                new HttpMediaTypeNotSupportedException("text/plain"));

        assertEquals(415, result.getCode());
    }

    @Test
    @DisplayName("handleNotLogin：返回 401")
    void handleNotLogin_returns401() {
        CommonResult<Void> result = handler.handleNotLogin(new NotLoginException("token", "未登录", null));

        assertEquals(401, result.getCode());
        assertEquals("未登录或登录已过期", result.getMessage());
    }

    @Test
    @DisplayName("handleIllegalState：返回 400")
    void handleIllegalState_returns400() {
        CommonResult<Void> result = handler.handleIllegalState(new IllegalStateException("状态错误"));

        assertEquals(400, result.getCode());
        assertEquals("状态错误", result.getMessage());
    }

    @Test
    @DisplayName("handleSecurity：返回 403")
    void handleSecurity_returns403() {
        CommonResult<Void> result = handler.handleSecurity(new SecurityException("非法文件路径"), request);

        assertEquals(403, result.getCode());
        assertEquals("非法文件路径", result.getMessage());
    }

    @Test
    @DisplayName("handleMissingPathVariable：返回缺失路径变量名")
    void handleMissingPathVariable_returnsVariableName() {
        MissingPathVariableException ex = missingPathVariableException("id");

        CommonResult<Void> result = handler.handleMissingPathVariable(ex);

        assertEquals(400, result.getCode());
        assertTrue(result.getMessage().contains("id"));
    }

    @Test
    @DisplayName("handleException：兜底返回 500 且不泄露细节")
    void handleException_returnsGenericMessage() {
        CommonResult<Void> result = handler.handleException(new RuntimeException("secret db error"), request);

        assertEquals(500, result.getCode());
        assertEquals("服务器内部错误", result.getMessage());
    }

    private static CommonResult<Void> requireBody(ResponseEntity<CommonResult<Void>> response) {
        return Objects.requireNonNull(response.getBody(), "response body");
    }

    private static MethodArgumentNotValidException validationException(BeanPropertyBindingResult bindingResult) {
        MethodParameter parameter = Objects.requireNonNull(mock(MethodParameter.class));
        return new MethodArgumentNotValidException(parameter, Objects.requireNonNull(bindingResult));
    }

    private static MissingPathVariableException missingPathVariableException(String name) {
        MethodParameter parameter = Objects.requireNonNull(mock(MethodParameter.class));
        return new MissingPathVariableException(Objects.requireNonNull(name), parameter);
    }
}

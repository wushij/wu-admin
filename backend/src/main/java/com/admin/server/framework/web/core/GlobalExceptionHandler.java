package com.admin.server.framework.web.core;

import cn.dev33.satoken.exception.NotLoginException;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.common.core.CommonResult;
import com.admin.server.common.util.ClientIpUtils;
import com.admin.server.framework.config.DynamicConfigProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.annotation.Resource;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Resource
    private DynamicConfigProvider dynamicConfigProvider;

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleIllegalState(IllegalStateException e) {
        log.warn("状态异常: {}", e.getMessage());
        return CommonResult.error(400, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数异常: {}", e.getMessage());
        return CommonResult.error(400, e.getMessage());
    }

    /** 自定义业务异常（携带错误码，HTTP 状态与 code 对齐） */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResult<Void>> handleBusiness(BusinessException e, HttpServletRequest request) {
        log.info("业务异常 [{} {}] code={} msg={} ip={}",
                request.getMethod(), request.getRequestURI(), e.getCode(), e.getMessage(),
                ClientIpUtils.resolve(request));
        HttpStatus status = BusinessHttpStatusMapper.toHttpStatus(e.getCode());
        return ResponseEntity.status(status.value()).body(CommonResult.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        int limit = dynamicConfigProvider.getFileMaxSizeMb();
        return CommonResult.error(400, "文件过大，单文件不能超过 " + limit + "MB（可在系统配置-文件存储中调整）");
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public CommonResult<Void> handleSecurity(SecurityException e, HttpServletRequest request) {
        log.warn("安全异常 [{} {}] ip={}: {}",
                request.getMethod(), request.getRequestURI(), ClientIpUtils.resolve(request), e.getMessage());
        return CommonResult.error(403, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public CommonResult<Void> handleAccessDenied(AccessDeniedException e) {
        return CommonResult.error(403, "权限不足，无法访问");
    }

    /** JSR-303 请求体校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return CommonResult.error(400, message.isEmpty() ? "参数验证失败" : message);
    }

    /** JSR-303 方法参数校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return CommonResult.error(400, message.isEmpty() ? "参数验证失败" : message);
    }

    /** JSON 请求体解析失败 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("JSON parse error: {}", e.getMessage());
        return CommonResult.error(400, "请求体格式错误，请检查 JSON 格式");
    }

    /** 请求参数缺失 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return CommonResult.error(400, "缺少必要参数: " + e.getParameterName());
    }

    /** 路径变量缺失 */
    @ExceptionHandler(MissingPathVariableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleMissingPathVariable(MissingPathVariableException e) {
        log.warn("路径变量缺失: {}", e.getVariableName());
        return CommonResult.error(400, "缺少路径参数: " + e.getVariableName());
    }

    /** 参数类型不匹配（如 String 传给了 Long 参数） */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return CommonResult.error(400, "参数类型错误: " + e.getName());
    }

    /** 404 路由不存在 */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CommonResult<Void> handleNoHandler(NoHandlerFoundException e) {
        return CommonResult.error(404, "请求的资源不存在");
    }

    /** HTTP 方法不支持 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public CommonResult<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return CommonResult.error(405, "请求方法不支持: " + e.getMethod());
    }

    /** Content-Type 不支持 */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public CommonResult<Void> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        return CommonResult.error(415, "不支持的媒体类型");
    }

    /** Sa-Token 未登录（注解鉴权等场景） */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public CommonResult<Void> handleNotLogin(NotLoginException e) {
        return CommonResult.error(401, "未登录或登录已过期");
    }

    /** 通用兜底异常处理 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResult<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("未处理异常 [{} {}] ip={}: {}",
                request.getMethod(), request.getRequestURI(), ClientIpUtils.resolve(request),
                e.getMessage(), e);
        return CommonResult.error(500, "服务器内部错误");
    }
}

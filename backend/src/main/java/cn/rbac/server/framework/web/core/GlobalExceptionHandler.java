package cn.rbac.server.framework.web.core;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.framework.config.DynamicConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
        return CommonResult.error(400, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleIllegalArgument(IllegalArgumentException e) {
        return CommonResult.error(400, e.getMessage());
    }

    /** 自定义业务异常（携带错误码） */
    @ExceptionHandler(BusinessException.class)
    public CommonResult<Void> handleBusiness(BusinessException e) {
        return CommonResult.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        int limit = dynamicConfigProvider.getFileMaxSizeMb();
        return CommonResult.error(400, "文件过大，单文件不能超过 " + limit + "MB（可在系统配置-文件存储中调整）");
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleSecurity(SecurityException e) {
        return CommonResult.error(400, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public CommonResult<Void> handleAccessDenied(AccessDeniedException e) {
        return CommonResult.error(403, "权限不足，无法访问");
    }

    /** JSR-303 参数验证失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
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

    /** 通用兑底异常处理 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResult<Void> handleException(Exception e) {
        log.error("未处理异常: {}", e.getMessage(), e);
        return CommonResult.error(500, "服务器内部错误");
    }
}

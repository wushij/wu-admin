package cn.rbac.server.framework.web.core;

import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.framework.config.DynamicConfigProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.annotation.Resource;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Resource
    private DynamicConfigProvider dynamicConfigProvider;

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleIllegalArgument(IllegalArgumentException e) {
        return CommonResult.error(400, e.getMessage());
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
}

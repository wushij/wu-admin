package cn.rbac.server.framework.web;

import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.modules.system.service.config.SystemConfigHelper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.annotation.Resource;

/**
 * 将业务校验异常转为 400 + 明确提示，避免前端只看到 500
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Resource
    private SystemConfigHelper systemConfigHelper;

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleIllegalArgument(IllegalArgumentException e) {
        return CommonResult.error(400, e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        int limit = systemConfigHelper.getFileMaxSizeMb();
        return CommonResult.error(400, "文件过大，单文件不能超过 " + limit + "MB（可在系统配置-文件存储中调整）");
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleSecurity(SecurityException e) {
        return CommonResult.error(400, e.getMessage());
    }
}

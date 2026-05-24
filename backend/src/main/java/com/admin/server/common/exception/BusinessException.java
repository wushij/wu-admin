package com.admin.server.common.exception;

import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 用于替代直接使用 IllegalArgumentException 传递业务错误信息。
 * 携带 HTTP 状态码 / 模块错误码和错误消息，由 GlobalExceptionHandler 统一处理。
 * </p>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode != null ? errorCode.msg() : "业务异常");
        this.code = errorCode != null ? errorCode.code() : 400;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode != null ? errorCode.msg() : "业务异常", cause);
        this.code = errorCode != null ? errorCode.code() : 400;
    }
}

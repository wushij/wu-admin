package com.admin.server.common.pojo;

import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 用于替代直接使用 IllegalArgumentException 传递业务错误信息。
 * 携带 HTTP 状态码和错误消息，由 GlobalExceptionHandler 统一处理。
 * </p>
 *
 * <pre>
 * // 用法示例
 * throw new BusinessException("用户名已存在");
 * throw new BusinessException(404, "工单不存在");
 * </pre>
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
}

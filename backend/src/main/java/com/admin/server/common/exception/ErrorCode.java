package com.admin.server.common.exception;

/**
 * 统一错误码结构
 *
 * @param code 错误码（例如 400, 401, 403, 10101001）
 * @param msg  错误描述
 */
public record ErrorCode(int code, String msg) {
}

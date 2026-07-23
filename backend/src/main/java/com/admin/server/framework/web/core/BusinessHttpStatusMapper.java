package com.admin.server.framework.web.core;

import org.springframework.http.HttpStatus;

/**
 * 业务错误码与 HTTP 状态码映射。
 */
public final class BusinessHttpStatusMapper {

    private BusinessHttpStatusMapper() {
    }

    public static HttpStatus toHttpStatus(int code) {
        return switch (code) {
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 405 -> HttpStatus.METHOD_NOT_ALLOWED;
            case 415 -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            case 500 -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}

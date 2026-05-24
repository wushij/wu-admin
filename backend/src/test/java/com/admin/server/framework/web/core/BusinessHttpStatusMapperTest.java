package com.admin.server.framework.web.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("BusinessHttpStatusMapper 单元测试")
class BusinessHttpStatusMapperTest {

    @Test
    @DisplayName("常见业务码映射 HTTP 状态")
    void mapsKnownCodes() {
        assertEquals(HttpStatus.UNAUTHORIZED, BusinessHttpStatusMapper.toHttpStatus(401));
        assertEquals(HttpStatus.FORBIDDEN, BusinessHttpStatusMapper.toHttpStatus(403));
        assertEquals(HttpStatus.NOT_FOUND, BusinessHttpStatusMapper.toHttpStatus(404));
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, BusinessHttpStatusMapper.toHttpStatus(405));
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, BusinessHttpStatusMapper.toHttpStatus(415));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, BusinessHttpStatusMapper.toHttpStatus(429));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, BusinessHttpStatusMapper.toHttpStatus(500));
        assertEquals(HttpStatus.BAD_REQUEST, BusinessHttpStatusMapper.toHttpStatus(400));
        assertEquals(HttpStatus.BAD_REQUEST, BusinessHttpStatusMapper.toHttpStatus(422));
    }
}

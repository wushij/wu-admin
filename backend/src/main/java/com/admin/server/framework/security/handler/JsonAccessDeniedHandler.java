package com.admin.server.framework.security.handler;

import com.admin.server.framework.web.core.ApiErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Spring Security 鉴权拒绝时返回 JSON 格式 {@link com.admin.server.common.pojo.CommonResult}。
 */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ApiErrorResponseWriter errorResponseWriter;

    public JsonAccessDeniedHandler(ApiErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        errorResponseWriter.write(response, 403, "权限不足，无法访问");
    }
}

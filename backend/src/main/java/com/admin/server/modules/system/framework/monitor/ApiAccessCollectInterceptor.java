package com.admin.server.modules.system.framework.monitor;

import com.admin.server.common.util.ClientIpUtils;
import com.admin.server.framework.security.core.service.SecurityUtils;
import com.admin.server.modules.system.dal.dataobject.monitor.ApiAccessLogDO;
import com.admin.server.modules.system.service.monitor.ApiAccessLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

@Slf4j
@Component
public class ApiAccessCollectInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "apiAccessStartTime";

    @Resource
    private ApiAccessLogService apiAccessLogService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler, @Nullable Exception ex) {
        try {
            String path = request.getRequestURI();
            if (shouldExclude(path)) {
                return;
            }

            Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
            if (startTime == null) {
                startTime = System.currentTimeMillis();
            }

            long costTime = System.currentTimeMillis() - startTime;
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTimeDt = endTime.minusNanos(costTime * 1_000_000L);

            ApiAccessLogDO accessLog = new ApiAccessLogDO();
            accessLog.setStartTime(startTimeDt);
            accessLog.setEndTime(endTime);
            accessLog.setApiPath(path);
            accessLog.setMethod(request.getMethod());
            accessLog.setStatusCode(response.getStatus());
            accessLog.setSuccess(response.getStatus() >= 200 && response.getStatus() < 400 ? 1 : 0);
            accessLog.setCostTime(costTime);
            accessLog.setIp(ClientIpUtils.resolve(request));

            Long userId = SecurityUtils.getLoginUserId();
            if (userId != null) {
                accessLog.setUserId(userId);
            }

            apiAccessLogService.pushToRedis(accessLog);
        } catch (Exception e) {
            log.warn("API 访问采集异常", e);
        }
    }

    private boolean shouldExclude(String path) {
        if (path == null) {
            return true;
        }
        return path.startsWith("/actuator")
                || path.startsWith("/druid")
                || path.startsWith("/error")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/doc.html")
                || path.startsWith("/webjars")
                || path.startsWith("/monitor/api-access")
                || path.contains(".");
    }
}

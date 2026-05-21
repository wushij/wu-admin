package cn.rbac.server.framework.web.interceptor;

import cn.rbac.server.framework.security.core.service.SecurityUtils;
import cn.rbac.server.framework.web.ClientIpUtils;
import cn.rbac.server.modules.system.dal.dataobject.monitor.ApiAccessLogDO;
import cn.rbac.server.modules.system.service.monitor.ApiAccessLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

/**
 * API 访问采集拦截器：异步写入 Redis，定时批量落库
 */
@Slf4j
@Component
public class ApiAccessCollectInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "apiAccessStartTime";

    @Resource
    private ApiAccessLogService apiAccessLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
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

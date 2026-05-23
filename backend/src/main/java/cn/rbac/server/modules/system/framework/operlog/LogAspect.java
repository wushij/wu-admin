package cn.rbac.server.modules.system.framework.operlog;

import cn.rbac.server.common.util.ClientIpUtils;
import cn.rbac.server.framework.log.annotation.Log;
import cn.rbac.server.framework.security.core.service.SecurityUtils;
import cn.rbac.server.modules.system.dal.dataobject.operlog.OperLogDO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

@Slf4j
@Aspect
@Component
public class LogAspect {

    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    @Resource
    private OperLogRecorder operLogRecorder;
    @Resource
    private ObjectMapper objectMapper;

    @Before("@annotation(controllerLog)")
    public void doBefore(JoinPoint joinPoint, Log controllerLog) {
        START_TIME.set(System.currentTimeMillis());
    }

    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Log controllerLog, Object jsonResult) {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }

    @AfterThrowing(pointcut = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Log controllerLog, Exception e) {
        handleLog(joinPoint, controllerLog, e, null);
    }

    protected void handleLog(JoinPoint joinPoint, Log controllerLog, Exception e, Object jsonResult) {
        try {
            OperLogDO operLog = new OperLogDO();
            operLog.setStatus(0);
            operLog.setOperTime(LocalDateTime.now());

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                operLog.setOperUrl(request.getRequestURI());
                operLog.setOperIp(ClientIpUtils.resolve(request));
                operLog.setRequestMethod(request.getMethod());
            }

            String username = SecurityUtils.getLoginUsername();
            if (username != null) {
                operLog.setOperName(username);
            }

            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");
            operLog.setTitle(controllerLog.title());
            operLog.setBusinessType(controllerLog.businessType().getValue());

            if (controllerLog.isSaveRequestData()) {
                setRequestValue(joinPoint, operLog);
            }

            if (controllerLog.isSaveResponseData() && jsonResult != null) {
                String result = objectMapper.writeValueAsString(jsonResult);
                operLog.setJsonResult(truncate(result, 2000));
            }

            if (e != null) {
                operLog.setStatus(1);
                operLog.setErrorMsg(truncate(e.getMessage(), 2000));
            }

            Long startTime = START_TIME.get();
            if (startTime != null) {
                operLog.setCostTime(System.currentTimeMillis() - startTime);
            }

            operLogRecorder.record(operLog);
        } catch (Exception ex) {
            log.error("记录操作日志异常", ex);
        } finally {
            START_TIME.remove();
        }
    }

    private void setRequestValue(JoinPoint joinPoint, OperLogDO operLog) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return;
            }
            StringBuilder params = new StringBuilder();
            for (Object arg : args) {
                if (arg != null && !isFilterObject(arg)) {
                    String jsonArg = objectMapper.writeValueAsString(arg);
                    params.append(maskSensitive(jsonArg)).append(' ');
                }
            }
            operLog.setOperParam(truncate(params.toString().trim(), 2000));
        } catch (Exception ex) {
            log.error("获取请求参数异常", ex);
        }
    }

    private String maskSensitive(String json) {
        if (json == null) {
            return null;
        }
        return json.replaceAll("(\"password\"\\s*:\\s*)\"[^\"]*\"", "$1\"******\"");
    }

    private boolean isFilterObject(Object obj) {
        if (obj instanceof MultipartFile || obj instanceof HttpServletRequest || obj instanceof HttpServletResponse) {
            return true;
        }
        Class<?> clazz = obj.getClass();
        if (clazz.isArray()) {
            return MultipartFile.class.isAssignableFrom(clazz.getComponentType());
        }
        if (Collection.class.isAssignableFrom(clazz)) {
            for (Object item : (Collection<?>) obj) {
                if (item instanceof MultipartFile) {
                    return true;
                }
            }
        }
        if (Map.class.isAssignableFrom(clazz)) {
            for (Object value : ((Map<?, ?>) obj).values()) {
                if (value instanceof MultipartFile) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}

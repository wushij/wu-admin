package com.admin.server.framework.quartz.util;

import com.admin.server.modules.system.dal.dataobject.job.SysJobDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Objects;

@Slf4j
public final class JobInvokeUtil {

    private JobInvokeUtil() {
    }

    public static void invokeMethod(SysJobDO job) throws Exception {
        String invokeTarget = job.getInvokeTarget();
        if (!StringUtils.hasText(invokeTarget)) {
            throw new IllegalArgumentException("调用目标不能为空");
        }
        if (!invokeTarget.contains(".")) {
            throw new IllegalArgumentException("调用目标格式错误，正确格式：beanName.methodName 或 beanName.methodName('参数')");
        }

        String beanName = getBeanName(invokeTarget);
        String methodName = getMethodName(invokeTarget);
        Object[] methodParams = getMethodParams(invokeTarget);

        Object bean = SpringUtils.getBean(beanName);
        if (methodParams != null && methodParams.length > 0) {
            Method method = bean.getClass().getMethod(methodName, getMethodParamsType(methodParams));
            method.invoke(bean, methodParams);
        } else {
            Method method = bean.getClass().getMethod(methodName);
            method.invoke(bean);
        }
    }

    private static @NonNull String getBeanName(String invokeTarget) {
        int dotIndex = invokeTarget.indexOf('.');
        if (dotIndex < 1) {
            throw new IllegalArgumentException("调用目标格式错误，缺少 bean 名称");
        }
        return Objects.requireNonNull(invokeTarget.substring(0, dotIndex));
    }

    private static @NonNull String getMethodName(String invokeTarget) {
        int dotIndex = invokeTarget.indexOf('.');
        if (dotIndex < 0 || dotIndex >= invokeTarget.length() - 1) {
            throw new IllegalArgumentException("调用目标格式错误，缺少方法名");
        }
        String methodName = invokeTarget.substring(dotIndex + 1);
        if (methodName.contains("(")) {
            methodName = methodName.substring(0, methodName.indexOf('('));
        }
        if (!StringUtils.hasText(methodName)) {
            throw new IllegalArgumentException("调用目标格式错误，方法名不能为空");
        }
        return Objects.requireNonNull(methodName);
    }

    private static Object[] getMethodParams(String invokeTarget) {
        if (!invokeTarget.contains("(") || !invokeTarget.contains(")")) {
            return null;
        }
        String params = invokeTarget.substring(invokeTarget.indexOf('(') + 1, invokeTarget.indexOf(')'));
        if (!StringUtils.hasText(params)) {
            return null;
        }
        String[] paramsArray = params.split(",");
        Object[] result = new Object[paramsArray.length];
        for (int i = 0; i < paramsArray.length; i++) {
            String param = paramsArray[i].trim();
            if ((param.startsWith("'") && param.endsWith("'")) || (param.startsWith("\"") && param.endsWith("\""))) {
                result[i] = param.substring(1, param.length() - 1);
            } else if ("true".equalsIgnoreCase(param) || "false".equalsIgnoreCase(param)) {
                result[i] = Boolean.parseBoolean(param);
            } else if (param.endsWith("L") || param.endsWith("l")) {
                result[i] = Long.parseLong(param.substring(0, param.length() - 1));
            } else if (param.endsWith("D") || param.endsWith("d")) {
                result[i] = Double.parseDouble(param.substring(0, param.length() - 1));
            } else {
                result[i] = Integer.parseInt(param);
            }
        }
        return result;
    }

    private static Class<?>[] getMethodParamsType(Object[] params) {
        Class<?>[] types = new Class<?>[params.length];
        for (int i = 0; i < params.length; i++) {
            if (params[i] instanceof String) {
                types[i] = String.class;
            } else if (params[i] instanceof Boolean) {
                types[i] = Boolean.class;
            } else if (params[i] instanceof Long) {
                types[i] = Long.class;
            } else if (params[i] instanceof Double) {
                types[i] = Double.class;
            } else if (params[i] instanceof Integer) {
                types[i] = Integer.class;
            } else {
                types[i] = params[i].getClass();
            }
        }
        return types;
    }
}

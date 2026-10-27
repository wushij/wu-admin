package com.admin.server.modules.infra.job.util;

import com.admin.server.framework.quartz.util.SpringUtils;
import com.admin.server.modules.infra.dal.dataobject.job.SysJobDO;
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
        int start = invokeTarget.indexOf('(');
        int end = invokeTarget.lastIndexOf(')');
        if (start < 0 || end < 0 || end <= start + 1) {
            return new Object[0];
        }
        String rawParams = invokeTarget.substring(start + 1, end).trim();
        if (!StringUtils.hasText(rawParams)) {
            return new Object[0];
        }

        String[] parts = rawParams.split(",");
        Object[] result = new Object[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = parseParam(parts[i].trim());
        }
        return result;
    }

    private static Object parseParam(String str) {
        if (str == null || str.isBlank()) {
            return "";
        }
        if ((str.startsWith("'") && str.endsWith("'")) || (str.startsWith("\"") && str.endsWith("\""))) {
            return str.substring(1, str.length() - 1);
        }
        if ("true".equalsIgnoreCase(str)) {
            return true;
        }
        if ("false".equalsIgnoreCase(str)) {
            return false;
        }
        if (str.endsWith("L") || str.endsWith("l")) {
            return Long.parseLong(str.substring(0, str.length() - 1));
        }
        if (str.endsWith("D") || str.endsWith("d")) {
            return Double.parseDouble(str.substring(0, str.length() - 1));
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException ignored) {
        }
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException ignored) {
        }
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException ignored) {
        }
        return str;
    }

    private static Class<?>[] getMethodParamsType(Object[] methodParams) {
        Class<?>[] classs = new Class<?>[methodParams.length];
        for (int i = 0; i < methodParams.length; i++) {
            classs[i] = methodParams[i].getClass();
        }
        return classs;
    }
}

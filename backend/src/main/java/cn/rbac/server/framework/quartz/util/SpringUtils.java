package cn.rbac.server.framework.quartz.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class SpringUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(@NonNull String name) {
        return (T) applicationContext.getBean(name);
    }

    public static <T> T getBean(@NonNull Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }
}

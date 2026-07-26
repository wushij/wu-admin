package com.admin.server.modules.infra.framework.monitor.config;

import com.admin.server.modules.infra.framework.monitor.ApiAccessCollectInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.Resource;

@Configuration
public class ApiAccessLogWebConfig implements WebMvcConfigurer {

    @Resource
    private ApiAccessCollectInterceptor apiAccessCollectInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        if (apiAccessCollectInterceptor != null) {
            registry.addInterceptor(apiAccessCollectInterceptor)
                    .addPathPatterns("/**")
                    .order(10);
        }
    }
}

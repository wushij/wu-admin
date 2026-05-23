package cn.rbac.server.modules.system.framework.monitor.config;

import cn.rbac.server.modules.system.framework.monitor.ApiAccessCollectInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.Resource;

@Configuration
public class ApiAccessLogWebConfig implements WebMvcConfigurer {

    @Resource
    private ApiAccessCollectInterceptor apiAccessCollectInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiAccessCollectInterceptor)
                .addPathPatterns("/**")
                .order(10);
    }
}

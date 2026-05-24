package com.admin.gateway.filter;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 全局认证过滤器
 */
@Configuration
public class SaTokenFilter {

    /**
     * 注册Sa-Token全局过滤器
     */
    @Bean
    public SaReactorFilter getSaReactorFilter() {
        return new SaReactorFilter()
                // 拦截地址
                .addInclude("/**")
                // 开放地址
                .addExclude("/favicon.ico")
                
                // 鉴权方法：每次访问进入
                .setAuth(obj -> {
                    // 登录认证 -- 保护所有 /api/** 请求
                    SaRouter.match("/api/**")
                            // 放行登录、注册、验证码、配置等公开接口
                            .notMatch(
                                    "/api/auth/login",
                                    "/api/auth/register",
                                    "/api/auth/captcha",
                                    "/api/auth/config"
                            )
                            // 其余接口统一要求登录
                            .check(r -> StpUtil.checkLogin());
                })
                
                // 异常处理方法：每次setAuth函数出现异常时进入
                .setError(e -> {
                    return SaResult.error(e.getMessage()).setCode(401);
                });
    }
}

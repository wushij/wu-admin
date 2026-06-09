package cn.rbac.server.framework.config;

import cn.dev33.satoken.config.SaCookieConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 写入 httpOnly Cookie，避免前端 localStorage 暴露 Token。
 */
@Configuration
public class SaTokenCookieConfig {

    @Bean
    public SaCookieConfig saCookieConfig(
            @Value("${app.security.cookie-secure:false}") boolean cookieSecure) {
        return new SaCookieConfig()
                .setPath("/")
                .setHttpOnly(true)
                .setSecure(cookieSecure)
                .setSameSite("Lax");
    }
}

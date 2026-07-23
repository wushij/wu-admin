package com.admin.server.framework.config;

import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

/**
 * 仅 dev：本地 Redis 常无密码，Redisson 对空串仍会 AUTH，需置 null。
 * prod 环境不加载，服务器仍用 application-prod.yml 的 password: root。
 */
@Configuration
@Profile("dev")
public class DevRedissonConfig {

    @Bean
    public RedissonAutoConfigurationCustomizer devRedissonCustomizer() {
        return (Config config) -> {
            if (!config.isSingleConfig()) {
                return;
            }
            SingleServerConfig server = config.useSingleServer();
            if (!StringUtils.hasText(server.getPassword())) {
                server.setPassword(null);
            }
        };
    }
}

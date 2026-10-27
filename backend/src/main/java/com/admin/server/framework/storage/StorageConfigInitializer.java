package com.admin.server.framework.storage;

import com.admin.server.framework.config.DynamicConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 应用启动时加载持久化文件存储配置并初始化驱动
 * <p>必须在配置缓存预热之后执行，避免读到 Redis 中的旧配置。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class StorageConfigInitializer implements ApplicationRunner {

    private final RoutingFileStorageService routingFileStorageService;
    private final DynamicConfigProvider dynamicConfigProvider;

    @Override
    public void run(ApplicationArguments args) {
        try {
            StorageConfig config = dynamicConfigProvider.getStorageConfig();
            if (config != null) {
                routingFileStorageService.reload(config);
                log.info("已成功从系统配置表加载存储引擎: type={}", config.getType());
            }
        } catch (Exception e) {
            log.warn("启动时加载存储引擎配置异常，保持默认本地驱动: {}", e.getMessage());
        }
    }
}

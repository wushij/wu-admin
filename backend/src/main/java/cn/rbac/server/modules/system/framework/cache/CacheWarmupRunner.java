package cn.rbac.server.modules.system.framework.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 应用启动后预热系统配置、字典全量 Redis 缓存
 */
@Slf4j
@Component
public class CacheWarmupRunner implements ApplicationRunner {

    @Resource
    private SysConfigCacheService sysConfigCacheService;
    @Resource
    private DictCacheService dictCacheService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始预热 Redis 业务缓存...");
        sysConfigCacheService.refreshAll();
        dictCacheService.refreshAll();
        log.info("Redis 业务缓存预热完成");
    }
}

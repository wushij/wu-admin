package cn.rbac.server.modules.system.task;

import cn.rbac.server.modules.system.service.monitor.ApiAccessLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 每 30 秒从 Redis 批量写入 MySQL
 */
@Slf4j
@Component
public class ApiAccessLogFlushTask {

    @Resource
    private ApiAccessLogService apiAccessLogService;

    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void flush() {
        apiAccessLogService.flushFromRedisToDb();
    }
}

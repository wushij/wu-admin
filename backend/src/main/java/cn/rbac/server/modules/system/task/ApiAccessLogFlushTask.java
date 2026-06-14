package cn.rbac.server.modules.system.task;

import cn.rbac.server.modules.system.service.monitor.ApiAccessLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 每 10 秒从 Redis 批量写入 MySQL（原 30 秒；高 QPS 时避免队列堆积）
 */
@Slf4j
@Component
public class ApiAccessLogFlushTask {

    @Resource
    private ApiAccessLogService apiAccessLogService;

    @Scheduled(fixedDelay = 10000, initialDelay = 10000)
    public void flush() {
        apiAccessLogService.flushFromRedisToDb();
    }
}

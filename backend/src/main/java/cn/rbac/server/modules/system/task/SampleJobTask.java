package cn.rbac.server.modules.system.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("sampleJobTask")
public class SampleJobTask {

    public void heartbeat() {
        log.info("定时任务心跳检测：调度器运行正常");
    }
}

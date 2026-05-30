package cn.rbac.server.modules.system.task;

import cn.rbac.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import cn.rbac.server.modules.system.dal.dataobject.monitor.ApiAccessLogDO;
import cn.rbac.server.modules.system.dal.dataobject.operlog.OperLogDO;
import cn.rbac.server.modules.system.dal.mysql.loginlog.LoginLogMapper;
import cn.rbac.server.modules.system.dal.mysql.monitor.ApiAccessLogMapper;
import cn.rbac.server.modules.system.dal.mysql.operlog.OperLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 定期清理过期日志（保留天数可在 application.yml 配置，0 表示不自动清理）
 */
@Slf4j
@Component
public class LogRetentionTask {

    @Value("${app.log.retention.oper-log-days:90}")
    private int operLogDays;

    @Value("${app.log.retention.login-log-days:90}")
    private int loginLogDays;

    @Value("${app.log.retention.api-access-log-days:30}")
    private int apiAccessLogDays;

    @Resource
    private OperLogMapper operLogMapper;
    @Resource
    private LoginLogMapper loginLogMapper;
    @Resource
    private ApiAccessLogMapper apiAccessLogMapper;

    /** 由 Quartz 定时任务 systemJobTask.purgeExpiredLogs 调用；也可在管理端手动执行 */
    public void purgeExpiredLogs() {
        purgeOperLogs();
        purgeLoginLogs();
        purgeApiAccessLogs();
    }

    private void purgeOperLogs() {
        if (operLogDays <= 0) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(operLogDays);
        int removed = operLogMapper.delete(new LambdaQueryWrapper<OperLogDO>()
                .lt(OperLogDO::getOperTime, cutoff));
        if (removed > 0) {
            log.info("操作日志归档清理：删除 {} 条（早于 {}）", removed, cutoff);
        }
    }

    private void purgeLoginLogs() {
        if (loginLogDays <= 0) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(loginLogDays);
        int removed = loginLogMapper.delete(new LambdaQueryWrapper<LoginLogDO>()
                .lt(LoginLogDO::getLoginTime, cutoff));
        if (removed > 0) {
            log.info("登录日志归档清理：删除 {} 条（早于 {}）", removed, cutoff);
        }
    }

    private void purgeApiAccessLogs() {
        if (apiAccessLogDays <= 0) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(apiAccessLogDays);
        int removed = apiAccessLogMapper.delete(new LambdaQueryWrapper<ApiAccessLogDO>()
                .lt(ApiAccessLogDO::getStartTime, cutoff));
        if (removed > 0) {
            log.info("API 访问日志归档清理：删除 {} 条（早于 {}）", removed, cutoff);
        }
    }
}

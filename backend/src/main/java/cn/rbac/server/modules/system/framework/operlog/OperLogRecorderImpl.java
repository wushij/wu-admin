package cn.rbac.server.modules.system.framework.operlog;

import cn.rbac.server.modules.system.dal.dataobject.operlog.OperLogDO;
import cn.rbac.server.modules.system.service.operlog.OperLogService;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

@Component
public class OperLogRecorderImpl implements OperLogRecorder {

    @Resource
    private OperLogService operLogService;

    @Override
    public void record(OperLogDO operLog) {
        operLogService.recordLog(operLog);
    }
}

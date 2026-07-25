package com.admin.server.modules.system.framework.operlog;

import com.admin.server.modules.system.dal.dataobject.operlog.OperLogDO;
import com.admin.server.modules.system.service.operlog.OperLogService;
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

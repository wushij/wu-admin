package com.admin.server.modules.infra.framework.operlog;

import com.admin.server.modules.infra.dal.dataobject.operlog.OperLogDO;
import com.admin.server.modules.infra.service.operlog.OperLogService;
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

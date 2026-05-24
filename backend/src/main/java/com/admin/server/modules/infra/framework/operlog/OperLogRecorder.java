package com.admin.server.modules.infra.framework.operlog;

import com.admin.server.modules.infra.dal.dataobject.operlog.OperLogDO;

/**
 * 操作日志落库 SPI（system 模块内实现）
 */
public interface OperLogRecorder {

    void record(OperLogDO operLog);
}

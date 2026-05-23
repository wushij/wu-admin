package cn.rbac.server.modules.system.framework.operlog;

import cn.rbac.server.modules.system.dal.dataobject.operlog.OperLogDO;

/**
 * 操作日志落库 SPI（system 模块内实现）
 */
public interface OperLogRecorder {

    void record(OperLogDO operLog);
}

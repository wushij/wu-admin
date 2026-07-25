package com.admin.server.modules.system.service.monitor;

import com.admin.server.modules.system.api.monitor.vo.ServerInfoVO;

/**
 * 本机服务监控（JMX）
 */
public interface ServerMonitorService {

    ServerInfoVO getInfo();
}

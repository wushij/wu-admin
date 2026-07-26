package com.admin.server.modules.infra.service.monitor;

import com.admin.server.modules.infra.api.monitor.vo.ServerInfoVO;

/**
 * 本机服务监控（JMX）
 */
public interface ServerMonitorService {

    ServerInfoVO getInfo();
}

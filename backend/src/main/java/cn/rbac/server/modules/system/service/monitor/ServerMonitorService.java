package cn.rbac.server.modules.system.service.monitor;

import cn.rbac.server.modules.system.api.monitor.vo.ServerInfoVO;

/**
 * 本机服务监控（JMX）
 */
public interface ServerMonitorService {

    ServerInfoVO getInfo();
}

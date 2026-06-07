package cn.rbac.server.modules.system.api.monitor.vo;

import lombok.Data;

import java.util.List;

/**
 * 本机服务监控（JMX 实时采集）
 */
@Data
public class ServerInfoVO {

    private ServerCpuVO cpu;
    private ServerMemoryVO memory;
    private ServerJvmVO jvm;
    private ServerSysVO sys;
    private List<ServerDiskVO> disks;
}

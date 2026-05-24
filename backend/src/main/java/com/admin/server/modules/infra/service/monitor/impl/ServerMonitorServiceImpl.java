package com.admin.server.modules.infra.service.monitor.impl;

import com.admin.server.modules.infra.api.monitor.vo.ServerCpuVO;
import com.admin.server.modules.infra.api.monitor.vo.ServerDiskVO;
import com.admin.server.modules.infra.api.monitor.vo.ServerInfoVO;
import com.admin.server.modules.infra.api.monitor.vo.ServerJvmVO;
import com.admin.server.modules.infra.api.monitor.vo.ServerMemoryVO;
import com.admin.server.modules.infra.api.monitor.vo.ServerSysVO;
import com.admin.server.modules.infra.service.monitor.ServerMonitorService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Service
public class ServerMonitorServiceImpl implements ServerMonitorService {

    /** JDK 21+ 替代已废弃的 getSystemCpuLoad() */
    private static final Method SYSTEM_CPU_LOAD_METHOD = resolveSystemCpuLoadMethod();

    private static Method resolveSystemCpuLoadMethod() {
        try {
            return com.sun.management.OperatingSystemMXBean.class.getMethod("getCpuLoad");
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Override
    public ServerInfoVO getInfo() {
        ServerInfoVO vo = new ServerInfoVO();
        vo.setCpu(buildCpu());
        vo.setMemory(buildMemory());
        vo.setJvm(buildJvm());
        vo.setSys(buildSys());
        vo.setDisks(buildDisks());
        return vo;
    }

    private ServerCpuVO buildCpu() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        ServerCpuVO cpu = new ServerCpuVO();
        cpu.setName(osBean.getName());
        cpu.setArch(osBean.getArch());
        cpu.setAvailableProcessors(osBean.getAvailableProcessors());
        double load = osBean.getSystemLoadAverage();
        cpu.setSystemLoadAverage(load >= 0 ? load : null);

        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            cpu.setSystemCpuPercent(readSystemCpuPercent(sunOs));
            cpu.setProcessCpuPercent(readProcessCpuPercent(sunOs));
        }
        return cpu;
    }

    private Double readSystemCpuPercent(com.sun.management.OperatingSystemMXBean sunOs) {
        if (SYSTEM_CPU_LOAD_METHOD != null) {
            try {
                Object value = SYSTEM_CPU_LOAD_METHOD.invoke(sunOs);
                if (value instanceof Double load) {
                    return toPercent(load);
                }
            } catch (ReflectiveOperationException ignored) {
                // fall through
            }
        }
        return readDeprecatedSystemCpuPercent(sunOs);
    }

    @SuppressWarnings("deprecation")
    private Double readDeprecatedSystemCpuPercent(com.sun.management.OperatingSystemMXBean sunOs) {
        return toPercent(sunOs.getSystemCpuLoad());
    }

    private Double readProcessCpuPercent(com.sun.management.OperatingSystemMXBean sunOs) {
        return toPercent(sunOs.getProcessCpuLoad());
    }

    private ServerMemoryVO buildMemory() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();

        ServerMemoryVO memory = new ServerMemoryVO();
        memory.setHeapInit(formatBytes(heap.getInit()));
        memory.setHeapUsed(formatBytes(heap.getUsed()));
        memory.setHeapMax(formatBytes(heap.getMax()));
        memory.setHeapCommitted(formatBytes(heap.getCommitted()));
        memory.setNonHeapUsed(formatBytes(nonHeap.getUsed()));
        memory.setHeapUsedBytes(heap.getUsed());
        memory.setHeapMaxBytes(heap.getMax() > 0 ? heap.getMax() : heap.getCommitted());
        memory.setHeapUsedPercent(calcPercent(heap.getUsed(), memory.getHeapMaxBytes()));

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            long total = sunOs.getTotalMemorySize();
            long free = sunOs.getFreeMemorySize();
            if (total > 0) {
                memory.setPhysicalTotal(formatBytes(total));
                memory.setPhysicalFree(formatBytes(free));
                memory.setPhysicalUsedPercent(calcPercent(total - free, total));
            }
        }
        return memory;
    }

    private ServerJvmVO buildJvm() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        ServerJvmVO jvm = new ServerJvmVO();
        jvm.setName(runtime.getVmName());
        jvm.setVendor(runtime.getVmVendor());
        jvm.setVersion(runtime.getVmVersion());
        jvm.setSpecVersion(runtime.getSpecVersion());
        jvm.setStartTime(formatTime(runtime.getStartTime()));
        jvm.setUptimeMillis(runtime.getUptime());
        jvm.setUptime(formatDuration(runtime.getUptime()));
        return jvm;
    }

    private ServerSysVO buildSys() {
        ServerSysVO sys = new ServerSysVO();
        try {
            InetAddress addr = InetAddress.getLocalHost();
            sys.setHostName(addr.getHostName());
            sys.setHostAddress(maskIp(addr.getHostAddress()));
        } catch (Exception ignored) {
            sys.setHostName("-");
            sys.setHostAddress("-");
        }
        sys.setOsName(System.getProperty("os.name", "-"));
        sys.setOsVersion(System.getProperty("os.version", "-"));
        sys.setUserDir(maskUserDir(System.getProperty("user.dir", "-")));
        sys.setJavaVersion(System.getProperty("java.version", "-"));
        return sys;
    }

    private String maskIp(String ip) {
        if (ip == null || ip.isBlank() || "-".equals(ip)) {
            return "-";
        }
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".***.***";
        }
        return "***";
    }

    private String maskUserDir(String userDir) {
        if (userDir == null || userDir.isBlank() || "-".equals(userDir)) {
            return "-";
        }
        File file = new File(userDir);
        String name = file.getName();
        return (name != null && !name.isBlank()) ? "***/" + name : "***";
    }

    private List<ServerDiskVO> buildDisks() {
        List<ServerDiskVO> disks = new ArrayList<>();
        for (File root : File.listRoots()) {
            long total = root.getTotalSpace();
            long free = root.getFreeSpace();
            long used = Math.max(total - free, 0);

            ServerDiskVO disk = new ServerDiskVO();
            disk.setPath(root.getPath());
            disk.setTotal(formatBytes(total));
            disk.setFree(formatBytes(free));
            disk.setUsed(formatBytes(used));
            disk.setUsedPercent(calcPercent(used, total));
            disks.add(disk);
        }
        return disks;
    }

    private Double toPercent(double load) {
        if (load < 0 || Double.isNaN(load)) {
            return null;
        }
        return Math.min(load * 100, 100.0);
    }

    private Double calcPercent(long used, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.min(used * 100.0 / total, 100.0);
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) {
            return "-";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        }
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String formatTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (days > 0) {
            return String.format("%d天%d小时%d分", days, hours, minutes);
        }
        if (hours > 0) {
            return String.format("%d小时%d分%d秒", hours, minutes, secs);
        }
        return String.format("%d分%d秒", minutes, secs);
    }
}

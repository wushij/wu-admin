package cn.rbac.server.modules.system.api.monitor;

import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.modules.system.api.monitor.vo.ServerInfoVO;
import cn.rbac.server.modules.system.service.monitor.ServerMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "服务监控")
@RestController
@RequestMapping("/monitor/server")
public class ServerMonitorController {

    @Resource
    private ServerMonitorService serverMonitorService;

    @Operation(summary = "本机 CPU / 内存 / JVM / 磁盘信息")
    @GetMapping("/info")
    @PreAuthorize("@ss.hasPermission('monitor:server:query')")
    public CommonResult<ServerInfoVO> info() {
        return CommonResult.success(serverMonitorService.getInfo());
    }
}

package cn.rbac.server.modules.system.api.monitor;

import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.modules.system.api.monitor.vo.OnlineUserVO;
import cn.rbac.server.modules.system.service.monitor.OnlineUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@Tag(name = "在线用户")
@RestController
@RequestMapping("/monitor/online")
public class OnlineUserController {

    @Resource
    private OnlineUserService onlineUserService;

    @Operation(summary = "在线用户列表")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('monitor:online:list')")
    public CommonResult<List<OnlineUserVO>> list() {
        return CommonResult.success(onlineUserService.listOnlineUsers());
    }

    @Operation(summary = "强退用户")
    @DeleteMapping("/{userId}")
    @PreAuthorize("@ss.hasPermission('monitor:online:forceLogout')")
    public CommonResult<Boolean> forceLogout(@PathVariable Long userId) {
        onlineUserService.forceLogout(userId);
        return CommonResult.success(true);
    }
}

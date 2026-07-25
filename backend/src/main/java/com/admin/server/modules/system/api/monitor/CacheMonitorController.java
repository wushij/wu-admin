package com.admin.server.modules.system.api.monitor;

import com.admin.server.common.pojo.CommonResult;
import com.admin.server.modules.system.api.monitor.vo.CacheInfoVO;
import com.admin.server.modules.system.api.monitor.vo.CacheKeysVO;
import com.admin.server.modules.system.api.monitor.vo.CacheStatsVO;
import com.admin.server.modules.system.api.monitor.vo.CacheValueVO;
import com.admin.server.modules.system.service.monitor.CacheMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "缓存监控")
@RestController
@RequestMapping("/monitor/cache")
public class CacheMonitorController {

    @Resource
    private CacheMonitorService cacheMonitorService;

    @Operation(summary = "Redis 实时统计（图表）")
    @GetMapping("/stats")
    @PreAuthorize("@ss.hasPermission('monitor:cache:query')")
    public CommonResult<CacheStatsVO> stats() {
        return CommonResult.success(cacheMonitorService.getStats());
    }

    @Operation(summary = "Redis 服务信息")
    @GetMapping("/info")
    @PreAuthorize("@ss.hasPermission('monitor:cache:query')")
    public CommonResult<CacheInfoVO> info() {
        return CommonResult.success(cacheMonitorService.getInfo());
    }

    @Operation(summary = "扫描缓存键（SCAN，上限 500）")
    @GetMapping("/keys")
    @PreAuthorize("@ss.hasPermission('monitor:cache:query')")
    public CommonResult<CacheKeysVO> keys(
            @RequestParam(defaultValue = "*") String pattern,
            @RequestParam(defaultValue = "200") Integer limit) {
        return CommonResult.success(cacheMonitorService.scanKeys(pattern, limit));
    }

    @Operation(summary = "查看缓存键详情")
    @GetMapping("/value")
    @PreAuthorize("@ss.hasPermission('monitor:cache:query')")
    public CommonResult<CacheValueVO> value(@RequestParam String key) {
        return CommonResult.success(cacheMonitorService.getValue(key));
    }

    @Operation(summary = "删除缓存键")
    @DeleteMapping
    @PreAuthorize("@ss.hasPermission('monitor:cache:delete')")
    public CommonResult<Boolean> delete(@RequestParam String key) {
        cacheMonitorService.deleteKey(key);
        return CommonResult.success(true);
    }
}

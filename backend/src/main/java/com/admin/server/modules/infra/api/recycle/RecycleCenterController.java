package com.admin.server.modules.infra.api.recycle;

import com.admin.server.common.core.CommonResult;
import com.admin.server.modules.infra.api.recycle.vo.RecycleSummaryVO;
import com.admin.server.modules.infra.service.recycle.RecycleCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "回收中心")
@RestController
@RequestMapping("/system/recycle")
public class RecycleCenterController {

    @Resource
    private RecycleCenterService recycleCenterService;

    @Operation(summary = "回收站汇总统计")
    @GetMapping("/summary")
    @PreAuthorize("@ss.hasRecycleRead()")
    public CommonResult<RecycleSummaryVO> summary() {
        return CommonResult.success(recycleCenterService.summary());
    }
}

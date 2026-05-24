package com.admin.server.modules.ai.api;

import com.admin.server.common.core.CommonResult;
import com.admin.server.common.core.PageResult;
import com.admin.server.framework.log.annotation.Log;
import com.admin.server.modules.ai.api.vo.AiChatLogPageReqVO;
import com.admin.server.modules.ai.api.vo.AiChatLogRespVO;
import com.admin.server.modules.ai.convert.AiModelConvert;
import com.admin.server.modules.ai.dal.dataobject.AiChatLogDO;
import com.admin.server.modules.ai.service.AiChatLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@Tag(name = "AI 对话日志")
@RestController
@RequestMapping("/system/ai-log")
public class AiChatLogController {

    @Resource
    private AiChatLogService aiChatLogService;

    @GetMapping("/page")
    @Operation(summary = "对话日志分页")
    @PreAuthorize("@ss.hasRead('system:ai-log:list')")
    public CommonResult<PageResult<AiChatLogRespVO>> page(AiChatLogPageReqVO reqVO) {
        PageResult<AiChatLogDO> page = aiChatLogService.page(reqVO);
        return CommonResult.success(PageResult.of(AiModelConvert.convertLogList(page.getList()), page.getTotal()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除对话日志")
    @PreAuthorize("@ss.hasPermission('system:ai-log:delete')")
    @Log(title = "AI对话日志", businessType = Log.BusinessType.DELETE)
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        aiChatLogService.delete(id);
        return CommonResult.success(true);
    }

    @DeleteMapping("/clean")
    @Operation(summary = "清空所有对话日志")
    @PreAuthorize("@ss.hasPermission('system:ai-log:delete')")
    @Log(title = "AI对话日志", businessType = Log.BusinessType.DELETE)
    public CommonResult<Boolean> clean() {
        aiChatLogService.cleanLogs();
        return CommonResult.success(true);
    }
}

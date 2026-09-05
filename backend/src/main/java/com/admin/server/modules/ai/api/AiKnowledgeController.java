package com.admin.server.modules.ai.api;

import com.admin.server.common.core.CommonResult;
import com.admin.server.common.core.PageResult;
import com.admin.server.framework.log.annotation.Log;
import com.admin.server.modules.ai.api.vo.AiKnowledgePageReqVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeRespVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeSaveReqVO;
import com.admin.server.modules.ai.service.AiKnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@Tag(name = "AI 知识库")
@RestController
@RequestMapping("/system/ai-knowledge")
public class AiKnowledgeController {

    @Resource
    private AiKnowledgeService aiKnowledgeService;

    @GetMapping("/page")
    @Operation(summary = "知识库分页")
    @PreAuthorize("@ss.hasRead('system:ai-knowledge:list')")
    public CommonResult<PageResult<AiKnowledgeRespVO>> page(AiKnowledgePageReqVO reqVO) {
        return CommonResult.success(aiKnowledgeService.page(reqVO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "知识详情")
    @PreAuthorize("@ss.hasRead('system:ai-knowledge:query')")
    public CommonResult<AiKnowledgeRespVO> detail(@PathVariable Long id) {
        return CommonResult.success(aiKnowledgeService.detail(id));
    }

    @PostMapping
    @Operation(summary = "新增知识")
    @PreAuthorize("@ss.hasPermission('system:ai-knowledge:create')")
    @Log(title = "AI知识库", businessType = Log.BusinessType.INSERT)
    public CommonResult<Boolean> create(@RequestBody AiKnowledgeSaveReqVO reqVO) {
        aiKnowledgeService.create(reqVO);
        return CommonResult.success(true);
    }

    @PutMapping
    @Operation(summary = "修改知识")
    @PreAuthorize("@ss.hasPermission('system:ai-knowledge:update')")
    @Log(title = "AI知识库", businessType = Log.BusinessType.UPDATE)
    public CommonResult<Boolean> update(@RequestBody AiKnowledgeSaveReqVO reqVO) {
        aiKnowledgeService.update(reqVO);
        return CommonResult.success(true);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识")
    @PreAuthorize("@ss.hasPermission('system:ai-knowledge:delete')")
    @Log(title = "AI知识库", businessType = Log.BusinessType.DELETE)
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        aiKnowledgeService.delete(id);
        return CommonResult.success(true);
    }
}

package com.admin.server.modules.ai.api;

import com.admin.server.common.core.CommonResult;
import com.admin.server.common.core.PageResult;
import com.admin.server.framework.log.annotation.Log;
import com.admin.server.framework.security.api.RequirePermission;
import com.admin.server.modules.ai.api.vo.AiKnowledgeChunkPageReqVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeChunkRespVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeChunkStatsVO;
import com.admin.server.modules.ai.service.AiKnowledgeChunkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 知识库（切片层）")
@RestController
@RequestMapping("/system/ai-knowledge-chunk")
public class AiKnowledgeChunkController {

    @Resource
    private AiKnowledgeChunkService chunkService;

    @GetMapping("/page")
    @Operation(summary = "切片分页")
    @RequirePermission(value = "system:ai-knowledge:list", type = RequirePermission.Type.READ)
    public CommonResult<PageResult<AiKnowledgeChunkRespVO>> page(AiKnowledgeChunkPageReqVO reqVO) {
        return CommonResult.success(chunkService.page(reqVO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "切片详情")
    @RequirePermission(value = "system:ai-knowledge:query", type = RequirePermission.Type.READ)
    public CommonResult<AiKnowledgeChunkRespVO> detail(@PathVariable Long id) {
        return CommonResult.success(chunkService.detail(id));
    }

    @GetMapping("/stats")
    @Operation(summary = "切片统计")
    @RequirePermission(value = "system:ai-knowledge:query", type = RequirePermission.Type.READ)
    public CommonResult<AiKnowledgeChunkStatsVO> stats(@RequestParam Long knowledgeBaseId) {
        return CommonResult.success(chunkService.stats(knowledgeBaseId));
    }

    @PostMapping("/{id}/reindex")
    @Operation(summary = "重建单个切片向量")
    @RequirePermission("system:ai-knowledge:reindex")
    @Log(title = "AI知识库切片", businessType = Log.BusinessType.UPDATE)
    public CommonResult<Boolean> reindex(@PathVariable Long id) {
        chunkService.reindex(id);
        return CommonResult.success(true);
    }
}

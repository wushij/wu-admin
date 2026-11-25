package com.admin.server.modules.ai.api;

import com.admin.server.common.core.CommonResult;
import com.admin.server.common.core.PageResult;
import com.admin.server.framework.log.annotation.Log;
import com.admin.server.framework.security.api.RequirePermission;
import com.admin.server.modules.ai.api.vo.AiKnowledgeBasePageReqVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeBaseRespVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeBaseSaveReqVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeChunkStatsVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeRetrieveReqVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeRetrieveRespVO;
import com.admin.server.modules.ai.api.vo.AiModelRespVO;
import com.admin.server.modules.ai.service.AiKnowledgeBaseService;
import com.admin.server.modules.ai.service.AiModelService;
import com.admin.server.modules.ai.service.rag.KnowledgeVectorSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "AI 知识库（知识库层）")
@RestController
@RequestMapping("/system/ai-knowledge-base")
public class AiKnowledgeBaseController {

    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_MIN_SCORE = 0.25;
    private static final int MAX_TOP_K = 20;

    @Resource
    private AiKnowledgeBaseService knowledgeBaseService;

    @Resource
    private AiModelService aiModelService;

    @Resource
    private KnowledgeVectorSearchService vectorSearchService;

    @GetMapping("/page")
    @Operation(summary = "知识库分页")
    @RequirePermission(value = "system:ai-knowledge:list", type = RequirePermission.Type.READ)
    public CommonResult<PageResult<AiKnowledgeBaseRespVO>> page(AiKnowledgeBasePageReqVO reqVO) {
        return CommonResult.success(knowledgeBaseService.page(reqVO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "知识库详情")
    @RequirePermission(value = "system:ai-knowledge:query", type = RequirePermission.Type.READ)
    public CommonResult<AiKnowledgeBaseRespVO> detail(@PathVariable Long id) {
        return CommonResult.success(knowledgeBaseService.detail(id));
    }

    @GetMapping("/list-enabled")
    @Operation(summary = "启用中的知识库列表")
    @RequirePermission(value = "system:ai-knowledge:list", type = RequirePermission.Type.READ)
    public CommonResult<List<AiKnowledgeBaseRespVO>> listEnabled() {
        return CommonResult.success(knowledgeBaseService.listEnabled());
    }

    @GetMapping("/embedding-models")
    @Operation(summary = "启用中的向量模型列表")
    @RequirePermission(value = "system:ai-knowledge:list", type = RequirePermission.Type.READ)
    public CommonResult<List<AiModelRespVO>> embeddingModels() {
        return CommonResult.success(aiModelService.listEnabledEmbedding());
    }

    @GetMapping("/{id}/chunk-stats")
    @Operation(summary = "知识库切片统计")
    @RequirePermission(value = "system:ai-knowledge:query", type = RequirePermission.Type.READ)
    public CommonResult<AiKnowledgeChunkStatsVO> chunkStats(@PathVariable Long id) {
        return CommonResult.success(knowledgeBaseService.chunkStats(id));
    }

    @PostMapping
    @Operation(summary = "新增知识库")
    @RequirePermission("system:ai-knowledge:create")
    @Log(title = "AI知识库", businessType = Log.BusinessType.INSERT)
    public CommonResult<Long> create(@RequestBody AiKnowledgeBaseSaveReqVO reqVO) {
        return CommonResult.success(knowledgeBaseService.create(reqVO));
    }

    @PutMapping
    @Operation(summary = "修改知识库")
    @RequirePermission("system:ai-knowledge:update")
    @Log(title = "AI知识库", businessType = Log.BusinessType.UPDATE)
    public CommonResult<Boolean> update(@RequestBody AiKnowledgeBaseSaveReqVO reqVO) {
        knowledgeBaseService.update(reqVO);
        return CommonResult.success(true);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识库及其文档与切片")
    @RequirePermission("system:ai-knowledge:delete")
    @Log(title = "AI知识库", businessType = Log.BusinessType.DELETE)
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        knowledgeBaseService.delete(id);
        return CommonResult.success(true);
    }

    @PostMapping("/retrieve-test")
    @Operation(summary = "知识库检索测试")
    @RequirePermission(value = "system:ai-knowledge:list", type = RequirePermission.Type.READ)
    public CommonResult<AiKnowledgeRetrieveRespVO> retrieveTest(@Valid @RequestBody AiKnowledgeRetrieveReqVO reqVO) {
        int topK = reqVO.getTopK() == null ? DEFAULT_TOP_K : Math.min(Math.max(reqVO.getTopK(), 1), MAX_TOP_K);
        double minScore = reqVO.getMinScore() == null || reqVO.getMinScore() < 0
                ? DEFAULT_MIN_SCORE : reqVO.getMinScore();
        return CommonResult.success(vectorSearchService.toResp(
                reqVO.getKnowledgeBaseId(), reqVO.getQuery().trim(), topK, minScore));
    }
}

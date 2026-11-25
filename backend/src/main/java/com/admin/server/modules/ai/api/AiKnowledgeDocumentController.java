package com.admin.server.modules.ai.api;

import com.admin.server.common.core.CommonResult;
import com.admin.server.common.core.PageResult;
import com.admin.server.framework.log.annotation.Log;
import com.admin.server.framework.security.api.RequirePermission;
import com.admin.server.modules.ai.api.vo.AiKnowledgeDocumentPageReqVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeDocumentRespVO;
import com.admin.server.modules.ai.service.AiKnowledgeDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "AI 知识库（文档层）")
@RestController
@RequestMapping("/system/ai-knowledge-document")
public class AiKnowledgeDocumentController {

    @Resource
    private AiKnowledgeDocumentService documentService;

    @GetMapping("/page")
    @Operation(summary = "文档分页")
    @RequirePermission(value = "system:ai-knowledge:list", type = RequirePermission.Type.READ)
    public CommonResult<PageResult<AiKnowledgeDocumentRespVO>> page(AiKnowledgeDocumentPageReqVO reqVO) {
        return CommonResult.success(documentService.page(reqVO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "文档详情")
    @RequirePermission(value = "system:ai-knowledge:query", type = RequirePermission.Type.READ)
    public CommonResult<AiKnowledgeDocumentRespVO> detail(@PathVariable Long id) {
        return CommonResult.success(documentService.detail(id));
    }

    @PostMapping("/upload")
    @Operation(summary = "上传知识文档（自动解析、切片、向量化）")
    @RequirePermission("system:ai-knowledge:upload")
    @Log(title = "AI知识库文档", businessType = Log.BusinessType.IMPORT)
    public CommonResult<List<Map<String, Object>>> upload(
            @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
            @RequestParam("files") MultipartFile[] files) {
        return CommonResult.success(documentService.upload(knowledgeBaseId, files));
    }

    @PostMapping("/{id}/reparse")
    @Operation(summary = "重新解析并重建切片")
    @RequirePermission("system:ai-knowledge:reindex")
    @Log(title = "AI知识库文档", businessType = Log.BusinessType.UPDATE)
    public CommonResult<Boolean> reparse(@PathVariable Long id) {
        documentService.reparse(id);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/reindex")
    @Operation(summary = "重新切片并向量化")
    @RequirePermission("system:ai-knowledge:reindex")
    @Log(title = "AI知识库文档", businessType = Log.BusinessType.UPDATE)
    public CommonResult<Boolean> reindex(@PathVariable Long id) {
        documentService.reindex(id);
        return CommonResult.success(true);
    }

    @PostMapping("/reindex-all")
    @Operation(summary = "重建整个知识库索引")
    @RequirePermission("system:ai-knowledge:reindex")
    @Log(title = "AI知识库", businessType = Log.BusinessType.UPDATE)
    public CommonResult<Boolean> reindexAll(@RequestParam Long knowledgeBaseId) {
        documentService.reindexAll(knowledgeBaseId);
        return CommonResult.success(true);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用/停用文档")
    @RequirePermission("system:ai-knowledge:update")
    @Log(title = "AI知识库文档", businessType = Log.BusinessType.UPDATE)
    public CommonResult<Boolean> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        documentService.updateStatus(id, status);
        return CommonResult.success(true);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档")
    @RequirePermission("system:ai-knowledge:delete")
    @Log(title = "AI知识库文档", businessType = Log.BusinessType.DELETE)
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        documentService.delete(id);
        return CommonResult.success(true);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除文档")
    @RequirePermission("system:ai-knowledge:delete")
    @Log(title = "AI知识库文档", businessType = Log.BusinessType.DELETE)
    public CommonResult<Boolean> batchDelete(@RequestBody List<Long> ids) {
        documentService.deleteBatch(ids);
        return CommonResult.success(true);
    }
}

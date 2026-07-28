package com.admin.server.modules.ai.api;

import com.admin.server.common.core.CommonResult;
import com.admin.server.common.core.PageResult;
import com.admin.server.framework.log.annotation.Log;
import com.admin.server.modules.ai.api.vo.AiModelPageReqVO;
import com.admin.server.modules.ai.api.vo.AiModelRespVO;
import com.admin.server.modules.ai.api.vo.AiModelSaveReqVO;
import com.admin.server.modules.ai.api.vo.AiModelTestReqVO;
import com.admin.server.modules.ai.service.AiModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@Tag(name = "AI 模型配置")
@RestController
@RequestMapping("/system/ai-model")
public class AiModelController {

    @Resource
    private AiModelService aiModelService;

    @GetMapping("/page")
    @Operation(summary = "模型配置分页")
    @PreAuthorize("@ss.hasRead('system:ai-model:list')")
    public CommonResult<PageResult<AiModelRespVO>> page(AiModelPageReqVO reqVO) {
        return CommonResult.success(aiModelService.page(reqVO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "模型配置详情")
    @PreAuthorize("@ss.hasRead('system:ai-model:list')")
    public CommonResult<AiModelRespVO> detail(@PathVariable Long id) {
        return CommonResult.success(aiModelService.detail(id));
    }

    @PostMapping
    @Operation(summary = "新增模型配置")
    @PreAuthorize("@ss.hasPermission('system:ai-model:create')")
    @Log(title = "AI模型配置", businessType = Log.BusinessType.INSERT)
    public CommonResult<Boolean> create(@RequestBody AiModelSaveReqVO reqVO) {
        aiModelService.create(reqVO);
        return CommonResult.success(true);
    }

    @PutMapping
    @Operation(summary = "修改模型配置")
    @PreAuthorize("@ss.hasPermission('system:ai-model:update')")
    @Log(title = "AI模型配置", businessType = Log.BusinessType.UPDATE)
    public CommonResult<Boolean> update(@RequestBody AiModelSaveReqVO reqVO) {
        aiModelService.update(reqVO);
        return CommonResult.success(true);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模型配置")
    @PreAuthorize("@ss.hasPermission('system:ai-model:delete')")
    @Log(title = "AI模型配置", businessType = Log.BusinessType.DELETE)
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        aiModelService.delete(id);
        return CommonResult.success(true);
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "设为默认模型")
    @PreAuthorize("@ss.hasPermission('system:ai-model:update')")
    @Log(title = "AI模型配置", businessType = Log.BusinessType.UPDATE)
    public CommonResult<Boolean> setDefault(@PathVariable Long id) {
        aiModelService.setDefault(id);
        return CommonResult.success(true);
    }

    @PostMapping("/test")
    @Operation(summary = "连通性测试，返回延迟毫秒数")
    @PreAuthorize("@ss.hasPermission('system:ai-model:test')")
    public CommonResult<Long> test(@RequestBody AiModelTestReqVO reqVO) {
        return CommonResult.success(aiModelService.test(reqVO));
    }
}

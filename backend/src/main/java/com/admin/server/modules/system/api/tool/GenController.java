package com.admin.server.modules.system.api.tool;

import com.admin.server.common.pojo.CommonResult;
import com.admin.server.common.pojo.PageParam;
import com.admin.server.common.pojo.PageResult;
import com.admin.server.framework.log.annotation.Log;
import com.admin.server.modules.system.dal.dataobject.gen.DatabaseTableVO;
import com.admin.server.modules.system.dal.dataobject.gen.GenTableDO;
import com.admin.server.modules.system.service.gen.GenTableService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Tag(name = "代码生成")
@RestController
@RequestMapping("/tool/gen")
@RequiredArgsConstructor
public class GenController {

    private final GenTableService genTableService;

    @GetMapping("/db/list")
    @Operation(summary = "可导入的数据库表")
    @PreAuthorize("@ss.hasRead('tool:gen:list')")
    public CommonResult<PageResult<DatabaseTableVO>> dbTableList(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String tableName) {
        Page<DatabaseTableVO> page = genTableService.selectDbTableList(pageNo, pageSize, tableName);
        return CommonResult.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @PostMapping("/import")
    @Operation(summary = "导入表结构")
    @PreAuthorize("@ss.hasPermission('tool:gen:import')")
    @Log(title = "代码生成", businessType = Log.BusinessType.INSERT)
    public CommonResult<Boolean> importTable(@RequestBody String[] tableNames) {
        genTableService.importTable(tableNames);
        return CommonResult.success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "已导入表分页")
    @PreAuthorize("@ss.hasRead('tool:gen:list')")
    public CommonResult<PageResult<GenTableDO>> page(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String tableName) {
        Page<GenTableDO> page = genTableService.page(pageNo, pageSize, tableName);
        return CommonResult.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "表配置详情")
    @PreAuthorize("@ss.hasRead('tool:gen:query')")
    public CommonResult<GenTableDO> getInfo(@PathVariable Long id) {
        return CommonResult.success(genTableService.getTableById(id));
    }

    @PutMapping
    @Operation(summary = "更新表配置")
    @PreAuthorize("@ss.hasPermission('tool:gen:edit')")
    @Log(title = "代码生成", businessType = Log.BusinessType.UPDATE)
    public CommonResult<Boolean> update(@RequestBody GenTableDO table) {
        genTableService.updateTable(table);
        return CommonResult.success(true);
    }

    @DeleteMapping("/{ids}")
    @Operation(summary = "删除表配置")
    @PreAuthorize("@ss.hasPermission('tool:gen:remove')")
    @Log(title = "代码生成", businessType = Log.BusinessType.DELETE)
    public CommonResult<Boolean> remove(@PathVariable Long[] ids) {
        genTableService.deleteTable(ids);
        return CommonResult.success(true);
    }

    @GetMapping("/preview/{id}")
    @Operation(summary = "预览代码")
    @PreAuthorize("@ss.hasPermission('tool:gen:preview')")
    public CommonResult<Map<String, String>> preview(@PathVariable Long id) {
        return CommonResult.success(genTableService.previewCode(id));
    }

    @GetMapping("/download")
    @Operation(summary = "下载代码 ZIP")
    @PreAuthorize("@ss.hasPermission('tool:gen:code')")
    @Log(title = "代码生成", businessType = Log.BusinessType.EXPORT)
    public void download(@RequestParam Long[] ids, HttpServletResponse response) throws IOException {
        byte[] data = genTableService.generateCode(ids);
        response.reset();
        response.setHeader("Content-Disposition", "attachment; filename=code.zip");
        response.setContentType("application/octet-stream");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
    }

    @GetMapping("/preview-generate/{id}")
    @Operation(summary = "预览将生成的文件")
    @PreAuthorize("@ss.hasPermission('tool:gen:code')")
    public CommonResult<List<String>> previewGenerateFiles(@PathVariable Long id) {
        return CommonResult.success(genTableService.previewGenerateFiles(id));
    }

    @PostMapping("/generate/{id}")
    @Operation(summary = "生成代码到项目")
    @PreAuthorize("@ss.hasPermission('tool:gen:code')")
    @Log(title = "代码生成", businessType = Log.BusinessType.INSERT)
    public CommonResult<List<String>> generateToProject(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean overwrite) {
        return CommonResult.success(genTableService.generateToProject(id, overwrite));
    }

    @GetMapping("/preview-remove/{id}")
    @Operation(summary = "预览将移除的文件")
    @PreAuthorize("@ss.hasPermission('tool:gen:code')")
    public CommonResult<List<String>> previewRemoveFiles(@PathVariable Long id) {
        return CommonResult.success(genTableService.previewRemoveFiles(id));
    }

    @DeleteMapping("/remove-code/{id}")
    @Operation(summary = "移除已生成代码")
    @PreAuthorize("@ss.hasPermission('tool:gen:code')")
    @Log(title = "代码生成", businessType = Log.BusinessType.DELETE)
    public CommonResult<List<String>> removeGeneratedCode(@PathVariable Long id) {
        return CommonResult.success(genTableService.removeGeneratedCode(id));
    }

    @PostMapping("/sync/{id}")
    @Operation(summary = "同步表结构")
    @PreAuthorize("@ss.hasPermission('tool:gen:edit')")
    @Log(title = "代码生成", businessType = Log.BusinessType.UPDATE)
    public CommonResult<Boolean> sync(@PathVariable Long id) {
        genTableService.syncTable(id);
        return CommonResult.success(true);
    }

    @GetMapping("/recycle/page")
    @Operation(summary = "代码生成回收站分页")
    @PreAuthorize("@ss.hasRecycleRead()")
    public CommonResult<PageResult<GenTableDO>> recyclePage(
            PageParam pageParam,
            @RequestParam(required = false) String tableName) {
        return CommonResult.success(genTableService.recyclePage(pageParam, tableName));
    }

    @PutMapping("/restore")
    @Operation(summary = "恢复代码生成表配置")
    @PreAuthorize("@ss.hasRecycleRestore('tool:gen:remove')")
    @Log(title = "代码生成", businessType = Log.BusinessType.UPDATE)
    public CommonResult<Boolean> restore(@RequestParam Long id) {
        genTableService.restore(id);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete-permanent")
    @Operation(summary = "彻底删除代码生成表配置")
    @PreAuthorize("@ss.hasRecycleDelete('tool:gen:remove')")
    @Log(title = "代码生成", businessType = Log.BusinessType.DELETE)
    public CommonResult<Boolean> deletePermanent(@RequestParam Long id) {
        genTableService.deletePermanent(id);
        return CommonResult.success(true);
    }
}

package com.admin.server.modules.infra.api.file;

import com.admin.server.framework.log.annotation.Log;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.common.core.CommonResult;
import com.admin.server.common.core.PageParam;
import com.admin.server.common.core.PageResult;
import com.admin.server.modules.infra.dal.dataobject.file.SysFileDO;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.admin.server.modules.infra.service.file.SysFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "文件管理")
@RestController
@RequestMapping("/system/file")
public class SysFileController {

    @Resource
    private SysFileService fileService;

    @Resource
    private SystemConfigHelper systemConfigHelper;

    @GetMapping("/upload-policy")
    @Operation(summary = "上传策略（读取系统配置，保存后立即生效）")
    @PreAuthorize("@ss.hasRead('sys:file:list')")
    public CommonResult<Map<String, Object>> uploadPolicy() {
        Map<String, Object> policy = new HashMap<>();
        policy.put("maxSizeMb", systemConfigHelper.getFileMaxSizeMb());
        policy.put("allowedExtensions", systemConfigHelper.getFileAllowedExtensions());
        policy.put("platformMaxMb", SystemConfigHelper.PLATFORM_MAX_FILE_MB);
        return CommonResult.success(policy);
    }

    @GetMapping("/page-by-group")
    @PreAuthorize("@ss.hasRead('sys:file:list')")
    public CommonResult<PageResult<SysFileDO>> pageByGroup(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Boolean ungrouped,
            @RequestParam(required = false) String fileCategory,
            @RequestParam(required = false) String originalName) {
        return CommonResult.success(
                fileService.pageByGroup(pageNo, pageSize, groupId, ungrouped, fileCategory, originalName));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasRead('sys:file:list')")
    public CommonResult<SysFileDO> detail(@PathVariable Long id) {
        return CommonResult.success(fileService.getById(id));
    }

    @Log(title = "文件管理", businessType = Log.BusinessType.INSERT, isSaveRequestData = false)
    @PostMapping("/upload")
    @PreAuthorize("@ss.hasPermission('sys:file:upload')")
    public CommonResult<SysFileDO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) Long groupId) {
        return CommonResult.success(fileService.upload(file, path, groupId));
    }

    @PostMapping("/upload/image")
    @PreAuthorize("@ss.hasPermission('sys:file:upload')")
    public CommonResult<SysFileDO> uploadImage(@RequestParam("file") MultipartFile file) {
        return CommonResult.success(fileService.uploadImage(file));
    }

    @GetMapping("/download/{id}")
    @PreAuthorize("@ss.hasRead('sys:file:list')")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long id) throws IOException {
        SysFileDO file = fileService.getById(id);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        org.springframework.core.io.Resource resource = fileService.openFileResource(id);
        String encoded = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .header(HttpHeaders.CONTENT_TYPE, contentTypeOf(file))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(resource);
    }

    @GetMapping("/preview/{id}")
    @PreAuthorize("@ss.hasRead('sys:file:list')")
    public ResponseEntity<org.springframework.core.io.Resource> preview(@PathVariable Long id) throws IOException {
        SysFileDO file = fileService.getById(id);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        org.springframework.core.io.Resource resource = fileService.openFileResource(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentTypeOf(file))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(resource);
    }

    private static String contentTypeOf(SysFileDO file) {
        return file.getFileType() != null ? file.getFileType() : "application/octet-stream";
    }

    @GetMapping("/text/{id}")
    @PreAuthorize("@ss.hasRead('sys:file:list')")
    public CommonResult<String> text(@PathVariable Long id) {
        SysFileDO file = fileService.getById(id);
        if (file == null) {
            throw new BusinessException(404, "文件不存在");
        }
        if (file.getFileSize() != null && file.getFileSize() > 5 * 1024 * 1024) {
            throw new BusinessException(400, "文件过大，无法预览");
        }
        return CommonResult.success(new String(fileService.getFileBytes(id), StandardCharsets.UTF_8));
    }

    @Log(title = "文件管理", businessType = Log.BusinessType.DELETE)
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermission('sys:file:delete')")
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        fileService.delete(id);
        return CommonResult.success(true);
    }

    @Log(title = "文件管理", businessType = Log.BusinessType.DELETE)
    @DeleteMapping("/batch")
    @PreAuthorize("@ss.hasPermission('sys:file:delete')")
    public CommonResult<Boolean> deleteBatch(@RequestBody Long[] ids) {
        fileService.deleteBatch(ids);
        return CommonResult.success(true);
    }

    @Log(title = "文件管理", businessType = Log.BusinessType.UPDATE)
    @PostMapping("/move")
    @PreAuthorize("@ss.hasPermission('sys:file:upload')")
    public CommonResult<Boolean> move(@RequestBody MoveFileRequest request) {
        fileService.moveToGroup(request.getFileIds(), request.getGroupId());
        return CommonResult.success(true);
    }

    @Log(title = "文件管理", businessType = Log.BusinessType.UPDATE)
    @PutMapping("/{id}/rename")
    @PreAuthorize("@ss.hasPermission('sys:file:upload')")
    public CommonResult<Boolean> rename(@PathVariable Long id, @RequestBody RenameRequest request) {
        fileService.rename(id, request.getNewName());
        return CommonResult.success(true);
    }

    @GetMapping("/recycle/page")
    @PreAuthorize("@ss.hasRecycleRead()")
    @Operation(summary = "文件回收站分页")
    public CommonResult<PageResult<SysFileDO>> recyclePage(
            PageParam pageParam,
            @RequestParam(required = false) String originalName) {
        return CommonResult.success(fileService.recyclePage(pageParam, originalName));
    }

    @PutMapping("/restore")
    @PreAuthorize("@ss.hasRecycleRestore('sys:file:delete')")
    @Operation(summary = "恢复文件")
    public CommonResult<Boolean> restore(@RequestParam Long id) {
        fileService.restore(id);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete-permanent")
    @PreAuthorize("@ss.hasRecycleDelete('sys:file:delete')")
    @Operation(summary = "彻底删除文件")
    public CommonResult<Boolean> deletePermanent(@RequestParam Long id) {
        fileService.deletePermanent(id);
        return CommonResult.success(true);
    }

    @Data
    public static class MoveFileRequest {
        private Long[] fileIds;
        private Long groupId;
    }

    @Data
    public static class RenameRequest {
        private String newName;
    }
}

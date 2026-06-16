package cn.rbac.server.modules.system.api.ticket;

import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.framework.log.annotation.Log;
import cn.rbac.server.framework.security.core.service.SecurityUtils;
import cn.rbac.server.modules.system.api.ticket.vo.*;
import cn.rbac.server.modules.system.dal.dataobject.ticket.TicketAttachmentDO;
import cn.rbac.server.modules.system.dal.dataobject.ticket.TicketCommentDO;
import cn.rbac.server.modules.system.dal.dataobject.ticket.TicketDO;
import cn.rbac.server.modules.system.service.ticket.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Tag(name = "工单管理")
@RestController
@RequestMapping("/system/ticket")
public class TicketController {

    @Resource
    private TicketService ticketService;

    @Operation(summary = "工单处理人选项")
    @GetMapping("/assignee-options")
    @PreAuthorize("@ss.hasRead('system:ticket:list')")
    public CommonResult<List<AssigneeOptionVO>> assigneeOptions() {
        return CommonResult.success(ticketService.getAssigneeOptions(SecurityUtils.getLoginUserIdOrZero()));
    }

    @Operation(summary = "工单分页")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasRead('system:ticket:list')")
    public CommonResult<PageResult<TicketDO>> page(PageParam pageParam,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long assigneeUserId) {
        return CommonResult.success(ticketService.page(pageParam, title, status, priority, assigneeUserId,
                SecurityUtils.getLoginUserIdOrZero()));
    }

    @Operation(summary = "工单详情")
    @GetMapping("/get")
    public CommonResult<TicketDO> get(@RequestParam Long id) {
        return CommonResult.success(ticketService.getDetail(id));
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "创建工单")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('system:ticket:create')")
    public CommonResult<Long> create(@Validated @RequestBody TicketCreateReqVO reqVO) {
        return CommonResult.success(ticketService.create(reqVO, SecurityUtils.getLoginUserIdOrZero()));
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "更新工单")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('system:ticket:update')")
    public CommonResult<Boolean> update(@Validated @RequestBody TicketUpdateReqVO reqVO) {
        ticketService.update(reqVO, SecurityUtils.getLoginUserIdOrZero());
        return CommonResult.success(true);
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除工单")
    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('system:ticket:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        ticketService.delete(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "回收站分页")
    @GetMapping("/recycle/page")
    @PreAuthorize("@ss.hasRecycleRead()")
    public CommonResult<PageResult<TicketDO>> recyclePage(PageParam pageParam,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        return CommonResult.success(ticketService.recyclePage(pageParam, title, status, priority));
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "恢复工单")
    @PutMapping("/restore")
    @PreAuthorize("@ss.hasRecycleRestore('system:ticket:delete')")
    public CommonResult<Boolean> restore(@RequestParam Long id) {
        ticketService.restore(id);
        return CommonResult.success(true);
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "彻底删除工单")
    @DeleteMapping("/delete-permanent")
    @PreAuthorize("@ss.hasRecycleDelete('system:ticket:delete')")
    public CommonResult<Boolean> deletePermanent(@RequestParam Long id) {
        ticketService.deletePermanent(id);
        return CommonResult.success(true);
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "流转工单状态")
    @PutMapping("/transition")
    public CommonResult<Boolean> transition(@Validated @RequestBody TicketTransitionReqVO reqVO) {
        ticketService.transition(reqVO, SecurityUtils.getLoginUserIdOrZero());
        return CommonResult.success(true);
    }

    @Operation(summary = "工单评论列表")
    @GetMapping("/comment/list")
    public CommonResult<List<TicketCommentDO>> commentList(@RequestParam Long ticketId) {
        return CommonResult.success(ticketService.listComments(ticketId));
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "新增工单评论")
    @PostMapping("/comment/create")
    public CommonResult<Long> createComment(@Validated @RequestBody TicketCommentCreateReqVO reqVO) {
        return CommonResult.success(ticketService.createComment(reqVO, SecurityUtils.getLoginUserIdOrZero()));
    }

    @Operation(summary = "工单附件列表")
    @GetMapping("/attachment/list")
    public CommonResult<List<TicketAttachmentDO>> attachmentList(@RequestParam Long ticketId) {
        return CommonResult.success(ticketService.listAttachments(ticketId));
    }

    @Operation(summary = "上传工单附件")
    @PostMapping(value = "/attachment/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResult<Long> uploadAttachment(@RequestParam Long ticketId,
                                               @RequestPart("file") MultipartFile file) throws IOException {
        return CommonResult.success(ticketService.uploadAttachment(ticketId, file, SecurityUtils.getLoginUserIdOrZero()));
    }

    @Operation(summary = "下载工单附件")
    @SuppressWarnings("all")
    @GetMapping("/attachment/download/{id}")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long id) throws IOException {
        TicketAttachmentDO attachment = ticketService.getAttachmentForDownload(id);
        if (attachment == null) {
            return ResponseEntity.notFound().build();
        }
        Path filePath = Paths.get(attachment.getFilePath());
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = Files.readAllBytes(filePath);
        String encodedName = URLEncoder.encode(attachment.getFileName(), StandardCharsets.UTF_8.name()).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }
}

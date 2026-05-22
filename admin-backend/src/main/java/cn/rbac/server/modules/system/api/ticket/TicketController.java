package cn.rbac.server.modules.system.api.ticket;

import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.dal.dataobject.notice.NoticeDO;
import cn.rbac.server.modules.system.dal.dataobject.ticket.TicketAttachmentDO;
import cn.rbac.server.modules.system.dal.dataobject.ticket.TicketCommentDO;
import cn.rbac.server.modules.system.dal.dataobject.ticket.TicketDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.dal.mysql.notice.NoticeMapper;
import cn.rbac.server.modules.system.dal.mysql.ticket.TicketAttachmentMapper;
import cn.rbac.server.modules.system.dal.mysql.ticket.TicketCommentMapper;
import cn.rbac.server.modules.system.dal.mysql.ticket.TicketMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.service.permission.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "工单管理")
@RestController
@RequestMapping("/system/ticket")
public class TicketController {

    @Resource
    private TicketMapper ticketMapper;
    @Resource
    private TicketCommentMapper ticketCommentMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private TicketAttachmentMapper ticketAttachmentMapper;
    @Resource
    private PermissionService permissionService;

    @Operation(summary = "工单处理人选项（不含敏感字段，供普通用户指派工单）")
    @GetMapping("/assignee-options")
    @PreAuthorize("@ss.hasRead('system:ticket:list')")
    public CommonResult<List<AssigneeOptionVO>> assigneeOptions() {
        List<UserDO> users = userMapper.selectList(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getStatus, 1)
                .select(UserDO::getId, UserDO::getUsername, UserDO::getNickname)
                .orderByAsc(UserDO::getUsername));
        List<AssigneeOptionVO> options = users.stream().map(u -> {
            AssigneeOptionVO vo = new AssigneeOptionVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setNickname(u.getNickname());
            return vo;
        }).collect(Collectors.toList());
        return CommonResult.success(options);
    }

    @Operation(summary = "工单分页")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasRead('system:ticket:list')")
    public CommonResult<PageResult<TicketDO>> page(PageParam pageParam,
                                                    @RequestParam(required = false) String title,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String priority,
                                                    @RequestParam(required = false) Long assigneeUserId) {
        Long userId = currentUserId();
        boolean isSuperAdmin = permissionService.hasRole(userId, "super_admin");
        LambdaQueryWrapper<TicketDO> wrapper = new LambdaQueryWrapper<>();
        if (title != null && !title.isEmpty()) {
            wrapper.like(TicketDO::getTitle, title);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(TicketDO::getStatus, status);
        }
        if (priority != null && !priority.isEmpty()) {
            wrapper.eq(TicketDO::getPriority, priority);
        }
        if (assigneeUserId != null) {
            wrapper.eq(TicketDO::getAssigneeUserId, assigneeUserId);
        }
        // 非超级管理员仅查看：我创建的 + 分配给我的工单
        if (!isSuperAdmin) {
            wrapper.and(w -> w.eq(TicketDO::getCreatorUserId, userId)
                    .or()
                    .eq(TicketDO::getAssigneeUserId, userId)
                    .or()
                    .eq(TicketDO::getAssigneeUserId, 0L));
        }
        wrapper.orderByDesc(TicketDO::getCreateTime);
        Page<TicketDO> page = ticketMapper.selectPage(new Page<>(pageParam.getPageNo(), pageParam.getPageSize()), wrapper);
        fillUserName(page.getRecords());
        return CommonResult.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @Operation(summary = "工单详情")
    @GetMapping("/get")
    public CommonResult<TicketDO> get(@RequestParam Long id) {
        TicketDO ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            return CommonResult.error(404, "工单不存在");
        }
        fillUserName(Collections.singletonList(ticket));
        return CommonResult.success(ticket);
    }

    @Operation(summary = "创建工单")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('system:ticket:create')")
    public CommonResult<Long> create(@RequestBody TicketCreateReqVO reqVO) {
        Long userId = currentUserId();
        Long assigneeUserId = reqVO.getAssigneeUserId();
        boolean allUsers = assigneeUserId != null && assigneeUserId.equals(0L);
        TicketDO ticket = new TicketDO();
        ticket.setTicketNo(generateTicketNo());
        ticket.setTitle(reqVO.getTitle());
        ticket.setDescription(reqVO.getDescription());
        ticket.setPriority(reqVO.getPriority() == null ? "MEDIUM" : reqVO.getPriority());
        ticket.setStatus("OPEN");
        ticket.setCreatorUserId(userId);
        ticket.setAssigneeUserId(allUsers ? 0L : assigneeUserId);
        ticket.setDeadline(reqVO.getDeadline());
        ticketMapper.insert(ticket);
        if (allUsers) {
            createBroadcastNotice(ticket, "你收到一条全员工单通知");
        } else {
            createAssignNotice(ticket, null, assigneeUserId, "工单已分配给你");
        }
        return CommonResult.success(ticket.getId());
    }

    @Operation(summary = "更新工单")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('system:ticket:update')")
    public CommonResult<Boolean> update(@RequestBody TicketUpdateReqVO reqVO) {
        TicketDO ticket = ticketMapper.selectById(reqVO.getId());
        if (ticket == null) {
            return CommonResult.error(404, "工单不存在");
        }
        Long newAssigneeUserId = reqVO.getAssigneeUserId();
        boolean allUsers = newAssigneeUserId != null && newAssigneeUserId.equals(0L);
        ticket.setTitle(reqVO.getTitle());
        ticket.setDescription(reqVO.getDescription());
        ticket.setPriority(reqVO.getPriority());
        Long oldAssigneeUserId = ticket.getAssigneeUserId();
        ticket.setAssigneeUserId(allUsers ? 0L : newAssigneeUserId);
        ticket.setDeadline(reqVO.getDeadline());
        ticketMapper.updateById(ticket);
        if (allUsers) {
            createBroadcastNotice(ticket, "一条工单已更新为全员通知");
        } else {
            createAssignNotice(ticket, oldAssigneeUserId, newAssigneeUserId, "工单处理人已变更，请及时跟进");
        }
        return CommonResult.success(true);
    }

    @Operation(summary = "删除工单")
    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('system:ticket:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        TicketDO ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            return CommonResult.error(404, "工单不存在");
        }
        // 同步删除评论和附件记录，避免残留孤儿数据
        ticketCommentMapper.delete(new LambdaQueryWrapper<TicketCommentDO>()
                .eq(TicketCommentDO::getTicketId, id));
        ticketAttachmentMapper.delete(new LambdaQueryWrapper<TicketAttachmentDO>()
                .eq(TicketAttachmentDO::getTicketId, id));
        ticketMapper.deleteById(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "回收站分页")
    @GetMapping("/recycle/page")
    @PreAuthorize("@ss.hasPermission('system:ticket:delete')")
    public CommonResult<PageResult<TicketDO>> recyclePage(PageParam pageParam,
                                                           @RequestParam(required = false) String title,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(required = false) String priority) {
        Page<TicketDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<TicketDO> deletedPage = (Page<TicketDO>) ticketMapper.selectDeletedPage(page, title, status, priority);
        fillUserName(deletedPage.getRecords());
        return CommonResult.success(PageResult.of(deletedPage.getRecords(), deletedPage.getTotal()));
    }

    @Operation(summary = "恢复工单")
    @PutMapping("/restore")
    @PreAuthorize("@ss.hasPermission('system:ticket:delete')")
    public CommonResult<Boolean> restore(@RequestParam Long id) {
        int rows = ticketMapper.restoreById(id);
        if (rows == 0) {
            return CommonResult.error(404, "回收站工单不存在");
        }
        ticketCommentMapper.restoreByTicketId(id);
        ticketAttachmentMapper.restoreByTicketId(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "彻底删除工单")
    @DeleteMapping("/delete-permanent")
    @PreAuthorize("@ss.hasPermission('system:ticket:delete')")
    public CommonResult<Boolean> deletePermanent(@RequestParam Long id) {
        ticketCommentMapper.deletePhysicalByTicketId(id);
        ticketAttachmentMapper.deletePhysicalByTicketId(id);
        int rows = ticketMapper.deletePhysicalById(id);
        if (rows == 0) {
            return CommonResult.error(404, "回收站工单不存在");
        }
        return CommonResult.success(true);
    }

    @Operation(summary = "流转工单状态")
    @PutMapping("/transition")
    public CommonResult<Boolean> transition(@RequestBody TicketTransitionReqVO reqVO) {
        TicketDO ticket = ticketMapper.selectById(reqVO.getId());
        if (ticket == null) {
            return CommonResult.error(404, "工单不存在");
        }
        Long userId = currentUserId();
        boolean hasTransitionPermission = permissionService.hasRole(userId, "super_admin")
                || permissionService.hasPermission(userId, "system:ticket:transition");
        boolean isAssignee = ticket.getAssigneeUserId() != null && ticket.getAssigneeUserId().equals(userId);
        if (!hasTransitionPermission && !isAssignee) {
            return CommonResult.error(403, "仅可流转分配给自己的工单");
        }
        String targetStatus = reqVO.getStatus();
        Set<String> allowedStatus = new HashSet<>(Arrays.asList("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"));
        if (!allowedStatus.contains(targetStatus)) {
            return CommonResult.error(400, "状态不合法");
        }
        ticket.setStatus(targetStatus);
        if ("CLOSED".equals(targetStatus)) {
            ticket.setClosedTime(LocalDateTime.now());
        }
        ticketMapper.updateById(ticket);
        return CommonResult.success(true);
    }

    @Operation(summary = "工单评论列表")
    @GetMapping("/comment/list")
    public CommonResult<List<TicketCommentDO>> commentList(@RequestParam Long ticketId) {
        List<TicketCommentDO> comments = ticketCommentMapper.selectList(
                new LambdaQueryWrapper<TicketCommentDO>()
                        .eq(TicketCommentDO::getTicketId, ticketId)
                        .orderByAsc(TicketCommentDO::getCreateTime)
        );
        fillCommentUsername(comments);
        return CommonResult.success(comments);
    }

    @Operation(summary = "新增工单评论")
    @PostMapping("/comment/create")
    public CommonResult<Long> createComment(@RequestBody TicketCommentCreateReqVO reqVO) {
        if (reqVO.getContent() == null || reqVO.getContent().trim().isEmpty()) {
            return CommonResult.error(400, "评论内容不能为空");
        }
        TicketDO ticket = ticketMapper.selectById(reqVO.getTicketId());
        if (ticket == null) {
            return CommonResult.error(404, "工单不存在");
        }
        TicketCommentDO comment = new TicketCommentDO();
        comment.setTicketId(reqVO.getTicketId());
        comment.setUserId(currentUserId());
        comment.setContent(reqVO.getContent().trim());
        ticketCommentMapper.insert(comment);
        return CommonResult.success(comment.getId());
    }

    @Operation(summary = "工单附件列表")
    @GetMapping("/attachment/list")
    public CommonResult<List<TicketAttachmentDO>> attachmentList(@RequestParam Long ticketId) {
        List<TicketAttachmentDO> attachments = ticketAttachmentMapper.selectList(
                new LambdaQueryWrapper<TicketAttachmentDO>()
                        .eq(TicketAttachmentDO::getTicketId, ticketId)
                        .orderByDesc(TicketAttachmentDO::getCreateTime)
        );
        fillAttachmentUploader(attachments);
        return CommonResult.success(attachments);
    }

    @Operation(summary = "上传工单附件")
    @PostMapping(value = "/attachment/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResult<Long> uploadAttachment(@RequestParam Long ticketId,
                                               @RequestPart("file") MultipartFile file) throws IOException {
        TicketDO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            return CommonResult.error(404, "工单不存在");
        }
        if (file == null || file.isEmpty()) {
            return CommonResult.error(400, "附件不能为空");
        }

        String originFileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "file";
        String safeName = UUID.randomUUID().toString().replace("-", "") + "_" + originFileName;
        Path uploadDir = Paths.get(System.getProperty("user.dir"), "data", "uploads", "ticket");
        Files.createDirectories(uploadDir);
        Path targetPath = uploadDir.resolve(safeName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        TicketAttachmentDO attachment = new TicketAttachmentDO();
        attachment.setTicketId(ticketId);
        attachment.setUploaderUserId(currentUserId());
        attachment.setFileName(originFileName);
        attachment.setFilePath(targetPath.toString());
        attachment.setFileSize(file.getSize());
        ticketAttachmentMapper.insert(attachment);
        return CommonResult.success(attachment.getId());
    }

    @Operation(summary = "下载工单附件")
    @GetMapping("/attachment/download/{id}")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long id) throws IOException {
        TicketAttachmentDO attachment = ticketAttachmentMapper.selectById(id);
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

    private String generateTicketNo() {
        return "TK" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return 0L;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        return Long.parseLong(principal.toString());
    }

    private void fillUserName(List<TicketDO> tickets) {
        Set<Long> userIds = tickets.stream()
                .flatMap(ticket -> java.util.stream.Stream.of(ticket.getCreatorUserId(), ticket.getAssigneeUserId()))
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserDO::getId, UserDO::getUsername, (a, b) -> a));
        tickets.forEach(ticket -> {
            if (ticket.getCreatorUserId() != null) {
                ticket.setCreatorName(userMap.getOrDefault(ticket.getCreatorUserId(), "-"));
            }
            if (ticket.getAssigneeUserId() != null) {
                if (ticket.getAssigneeUserId().equals(0L)) {
                    ticket.setAssigneeName("全部人员");
                } else {
                    ticket.setAssigneeName(userMap.getOrDefault(ticket.getAssigneeUserId(), "-"));
                }
            }
        });
    }

    private void fillCommentUsername(List<TicketCommentDO> comments) {
        Set<Long> userIds = comments.stream().map(TicketCommentDO::getUserId).collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserDO::getId, UserDO::getUsername, (a, b) -> a));
        comments.forEach(comment -> comment.setUsername(userMap.getOrDefault(comment.getUserId(), "-")));
    }

    private void fillAttachmentUploader(List<TicketAttachmentDO> attachments) {
        Set<Long> userIds = attachments.stream().map(TicketAttachmentDO::getUploaderUserId).collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserDO::getId, UserDO::getUsername, (a, b) -> a));
        attachments.forEach(attachment -> attachment.setUploaderName(userMap.getOrDefault(attachment.getUploaderUserId(), "-")));
    }

    private void createAssignNotice(TicketDO ticket, Long oldAssigneeUserId, Long newAssigneeUserId, String content) {
        if (newAssigneeUserId == null || newAssigneeUserId <= 0) {
            return;
        }
        if (oldAssigneeUserId != null && oldAssigneeUserId.equals(newAssigneeUserId)) {
            return;
        }
        if (newAssigneeUserId.equals(currentUserId())) {
            return;
        }
        NoticeDO notice = new NoticeDO();
        notice.setUserId(newAssigneeUserId);
        notice.setTitle("工单分配通知");
        notice.setContent(content + "：" + ticket.getTicketNo() + " - " + ticket.getTitle());
        notice.setBizType("TICKET");
        notice.setBizId(ticket.getId());
        notice.setReadStatus(0);
        noticeMapper.insert(notice);
    }

    private void createBroadcastNotice(TicketDO ticket, String content) {
        List<UserDO> users = userMapper.selectList(new LambdaQueryWrapper<UserDO>().eq(UserDO::getStatus, 1));
        Long currentUserId = currentUserId();
        for (UserDO user : users) {
            if (user.getId() == null || user.getId() <= 0 || user.getId().equals(currentUserId)) {
                continue;
            }
            NoticeDO notice = new NoticeDO();
            notice.setUserId(user.getId());
            notice.setTitle("工单全员通知");
            notice.setContent(content + "：" + ticket.getTicketNo() + " - " + ticket.getTitle());
            notice.setBizType("TICKET");
            notice.setBizId(ticket.getId());
            notice.setReadStatus(0);
            noticeMapper.insert(notice);
        }
    }

    @Data
    public static class TicketCreateReqVO {
        private String title;
        private String description;
        private String priority;
        private Long assigneeUserId;
        private LocalDateTime deadline;
    }

    @Data
    public static class TicketUpdateReqVO {
        private Long id;
        private String title;
        private String description;
        private String priority;
        private Long assigneeUserId;
        private LocalDateTime deadline;
    }

    @Data
    public static class TicketTransitionReqVO {
        private Long id;
        private String status;
    }

    @Data
    public static class TicketCommentCreateReqVO {
        private Long ticketId;
        private String content;
    }

    @Data
    public static class AssigneeOptionVO {
        private Long id;
        private String username;
        private String nickname;
    }
}

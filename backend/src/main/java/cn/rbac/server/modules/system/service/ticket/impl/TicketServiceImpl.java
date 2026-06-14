package cn.rbac.server.modules.system.service.ticket.impl;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.api.ticket.vo.*;
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
import cn.rbac.server.modules.system.service.notice.NoticeService;
import cn.rbac.server.modules.system.service.permission.PermissionService;
import cn.rbac.server.modules.system.service.ticket.TicketService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TicketServiceImpl implements TicketService {

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
    @Resource
    private NoticeService noticeService;

    @Override
    public List<AssigneeOptionVO> getAssigneeOptions(Long currentUserId) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getStatus, 1)
                .select(UserDO::getId, UserDO::getUsername, UserDO::getNickname)
                .orderByAsc(UserDO::getUsername);
        if (currentUserId != null && currentUserId > 0) {
            wrapper.ne(UserDO::getId, currentUserId);
        }
        return userMapper.selectList(wrapper).stream().map(u -> {
            AssigneeOptionVO vo = new AssigneeOptionVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setNickname(u.getNickname());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public PageResult<TicketDO> page(PageParam pageParam, String title, String status,
                                      String priority, Long assigneeUserId, Long currentUserId) {
        boolean isSuperAdmin = permissionService.hasRole(currentUserId, "super_admin");
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
        if (!isSuperAdmin) {
            wrapper.and(w -> w.eq(TicketDO::getCreatorUserId, currentUserId)
                    .or().eq(TicketDO::getAssigneeUserId, currentUserId)
                    .or().eq(TicketDO::getAssigneeUserId, 0L));
        }
        wrapper.orderByDesc(TicketDO::getCreateTime);
        Page<TicketDO> page = ticketMapper.selectPage(new Page<>(pageParam.getPageNo(), pageParam.getPageSize()), wrapper);
        fillUserName(page.getRecords());
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    @Override
    public TicketDO getDetail(Long id) {
        TicketDO ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BusinessException(404, "工单不存在");
        }
        fillUserName(Collections.singletonList(ticket));
        return ticket;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TicketCreateReqVO reqVO, Long currentUserId) {
        Long assigneeUserId = reqVO.getAssigneeUserId();
        boolean allUsers = assigneeUserId != null && assigneeUserId.equals(0L);
        if (!allUsers) {
            if (assigneeUserId == null || assigneeUserId <= 0) {
                throw new BusinessException(400, "请选择处理人");
            }
            if (currentUserId != null && currentUserId > 0 && assigneeUserId.equals(currentUserId)) {
                throw new BusinessException(400, "不能选择自己作为处理人");
            }
        }
        TicketDO ticket = new TicketDO();
        ticket.setTicketNo(generateTicketNo());
        ticket.setTitle(reqVO.getTitle());
        ticket.setDescription(reqVO.getDescription());
        ticket.setPriority(reqVO.getPriority() == null ? "MEDIUM" : reqVO.getPriority());
        ticket.setStatus("OPEN");
        ticket.setCreatorUserId(currentUserId);
        ticket.setAssigneeUserId(allUsers ? Long.valueOf(0L) : assigneeUserId);
        ticket.setDeadline(reqVO.getDeadline());
        ticketMapper.insert(ticket);
        if (allUsers) {
            createBroadcastNotice(ticket, "你收到一条全员工单通知");
        } else {
            createAssignNotice(ticket, null, assigneeUserId, "工单已分配给你");
        }
        return ticket.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TicketUpdateReqVO reqVO, Long currentUserId) {
        TicketDO ticket = ticketMapper.selectById(reqVO.getId());
        if (ticket == null) {
            throw new BusinessException(404, "工单不存在");
        }
        Long newAssigneeUserId = reqVO.getAssigneeUserId();
        boolean allUsers = newAssigneeUserId != null && newAssigneeUserId.equals(0L);
        ticket.setTitle(reqVO.getTitle());
        ticket.setDescription(reqVO.getDescription());
        ticket.setPriority(reqVO.getPriority());
        Long oldAssigneeUserId = ticket.getAssigneeUserId();
        ticket.setAssigneeUserId(allUsers ? Long.valueOf(0L) : newAssigneeUserId);
        ticket.setDeadline(reqVO.getDeadline());
        ticketMapper.updateById(ticket);
        if (allUsers) {
            createBroadcastNotice(ticket, "一条工单已更新为全员通知");
        } else {
            createAssignNotice(ticket, oldAssigneeUserId, newAssigneeUserId, "工单处理人已变更，请及时跟进");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        TicketDO ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BusinessException(404, "工单不存在");
        }
        ticketCommentMapper.delete(new LambdaQueryWrapper<TicketCommentDO>().eq(TicketCommentDO::getTicketId, id));
        ticketAttachmentMapper.delete(new LambdaQueryWrapper<TicketAttachmentDO>().eq(TicketAttachmentDO::getTicketId, id));
        ticketMapper.deleteById(id);
    }

    @Override
    public PageResult<TicketDO> recyclePage(PageParam pageParam, String title, String status, String priority) {
        Page<TicketDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<TicketDO> deletedPage = (Page<TicketDO>) ticketMapper.selectDeletedPage(page, title, status, priority);
        fillUserName(deletedPage.getRecords());
        return PageResult.of(deletedPage.getRecords(), deletedPage.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        int rows = ticketMapper.restoreById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站工单不存在");
        }
        ticketCommentMapper.restoreByTicketId(id);
        ticketAttachmentMapper.restoreByTicketId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermanent(Long id) {
        ticketCommentMapper.deletePhysicalByTicketId(id);
        ticketAttachmentMapper.deletePhysicalByTicketId(id);
        int rows = ticketMapper.deletePhysicalById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站工单不存在");
        }
    }

    @Override
    public void transition(TicketTransitionReqVO reqVO, Long currentUserId) {
        TicketDO ticket = ticketMapper.selectById(reqVO.getId());
        if (ticket == null) {
            throw new BusinessException(404, "工单不存在");
        }
        boolean hasTransitionPermission = permissionService.hasRole(currentUserId, "super_admin")
                || permissionService.hasPermission(currentUserId, "system:ticket:transition");
        boolean isAssignee = ticket.getAssigneeUserId() != null && ticket.getAssigneeUserId().equals(currentUserId);
        if (!hasTransitionPermission && !isAssignee) {
            throw new BusinessException(403, "仅可流转分配给自己的工单");
        }
        String targetStatus = reqVO.getStatus();
        Set<String> allowedStatus = new HashSet<>(Arrays.asList("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"));
        if (!allowedStatus.contains(targetStatus)) {
            throw new BusinessException(400, "状态不合法");
        }
        ticket.setStatus(targetStatus);
        if ("CLOSED".equals(targetStatus)) {
            ticket.setClosedTime(LocalDateTime.now());
        }
        ticketMapper.updateById(ticket);
        noticeService.markReadByBiz(currentUserId, "TICKET", ticket.getId());
    }

    @Override
    public List<TicketCommentDO> listComments(Long ticketId) {
        List<TicketCommentDO> comments = ticketCommentMapper.selectList(
                new LambdaQueryWrapper<TicketCommentDO>()
                        .eq(TicketCommentDO::getTicketId, ticketId)
                        .orderByAsc(TicketCommentDO::getCreateTime));
        fillCommentUsername(comments);
        return comments;
    }

    @Override
    public Long createComment(TicketCommentCreateReqVO reqVO, Long currentUserId) {
        TicketDO ticket = ticketMapper.selectById(reqVO.getTicketId());
        if (ticket == null) {
            throw new BusinessException(404, "工单不存在");
        }
        TicketCommentDO comment = new TicketCommentDO();
        comment.setTicketId(reqVO.getTicketId());
        comment.setUserId(currentUserId);
        comment.setContent(reqVO.getContent().trim());
        ticketCommentMapper.insert(comment);
        return comment.getId();
    }

    @Override
    public List<TicketAttachmentDO> listAttachments(Long ticketId) {
        List<TicketAttachmentDO> attachments = ticketAttachmentMapper.selectList(
                new LambdaQueryWrapper<TicketAttachmentDO>()
                        .eq(TicketAttachmentDO::getTicketId, ticketId)
                        .orderByDesc(TicketAttachmentDO::getCreateTime));
        fillAttachmentUploader(attachments);
        return attachments;
    }

    @Override
    public Long uploadAttachment(Long ticketId, MultipartFile file, Long currentUserId) throws IOException {
        TicketDO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new BusinessException(404, "工单不存在");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "附件不能为空");
        }
        String originFileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "file";
        String safeName = UUID.randomUUID().toString().replace("-", "") + "_" + originFileName;
        Path uploadDir = Paths.get(System.getProperty("user.dir"), "data", "uploads", "ticket");
        Files.createDirectories(uploadDir);
        Path targetPath = uploadDir.resolve(safeName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        TicketAttachmentDO attachment = new TicketAttachmentDO();
        attachment.setTicketId(ticketId);
        attachment.setUploaderUserId(currentUserId);
        attachment.setFileName(originFileName);
        attachment.setFilePath(targetPath.toString());
        attachment.setFileSize(file.getSize());
        ticketAttachmentMapper.insert(attachment);
        return attachment.getId();
    }

    @Override
    public TicketAttachmentDO getAttachmentForDownload(Long id) {
        return ticketAttachmentMapper.selectById(id);
    }

    // ---- 私有方法 ----

    private String generateTicketNo() {
        return "TK" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private void fillUserName(List<TicketDO> tickets) {
        Set<Long> userIds = tickets.stream()
                .flatMap(ticket -> java.util.stream.Stream.of(ticket.getCreatorUserId(), ticket.getAssigneeUserId()))
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) return;
        List<UserDO> userList = userMapper.selectByIds(userIds);
        Map<Long, String> userMap = (userList != null ? userList : Collections.<UserDO>emptyList()).stream()
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
        if (userIds.isEmpty()) return;
        List<UserDO> userList = userMapper.selectByIds(userIds);
        Map<Long, String> userMap = (userList != null ? userList : Collections.<UserDO>emptyList()).stream()
                .collect(Collectors.toMap(UserDO::getId, UserDO::getUsername, (a, b) -> a));
        comments.forEach(comment -> comment.setUsername(userMap.getOrDefault(comment.getUserId(), "-")));
    }

    private void fillAttachmentUploader(List<TicketAttachmentDO> attachments) {
        Set<Long> userIds = attachments.stream().map(TicketAttachmentDO::getUploaderUserId).collect(Collectors.toSet());
        if (userIds.isEmpty()) return;
        List<UserDO> userList = userMapper.selectByIds(userIds);
        Map<Long, String> userMap = (userList != null ? userList : Collections.<UserDO>emptyList()).stream()
                .collect(Collectors.toMap(UserDO::getId, UserDO::getUsername, (a, b) -> a));
        attachments.forEach(a -> a.setUploaderName(userMap.getOrDefault(a.getUploaderUserId(), "-")));
    }

    private void createAssignNotice(TicketDO ticket, Long oldAssigneeUserId, Long newAssigneeUserId, String content) {
        if (newAssigneeUserId == null || newAssigneeUserId <= 0) return;
        if (oldAssigneeUserId != null && oldAssigneeUserId.equals(newAssigneeUserId)) return;
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
        if (users.isEmpty()) {
            return;
        }
        String noticeContent = content + "：" + ticket.getTicketNo() + " - " + ticket.getTitle();
        LocalDateTime now = LocalDateTime.now();
        List<NoticeDO> notices = new ArrayList<>();
        for (UserDO user : users) {
            if (user.getId() == null || user.getId() <= 0) {
                continue;
            }
            NoticeDO notice = new NoticeDO();
            notice.setUserId(user.getId());
            notice.setTitle("工单全员通知");
            notice.setContent(noticeContent);
            notice.setBizType("TICKET");
            notice.setBizId(ticket.getId());
            notice.setReadStatus(0);
            notice.setCreateTime(now);
            notice.setUpdateTime(now);
            notices.add(notice);
        }
        if (!notices.isEmpty()) {
            noticeMapper.insertBatch(notices);
        }
    }
}

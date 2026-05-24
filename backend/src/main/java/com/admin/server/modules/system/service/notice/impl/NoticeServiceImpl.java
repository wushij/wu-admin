package com.admin.server.modules.system.service.notice.impl;

import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.system.dal.dataobject.notice.NoticeDO;
import com.admin.server.modules.system.dal.mysql.notice.NoticeMapper;
import com.admin.server.modules.system.service.notice.NoticeService;
import com.admin.server.modules.ticket.dal.mysql.approval.ApprovalFormMapper;
import com.admin.server.modules.ticket.dal.mysql.ticket.TicketMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeServiceImpl implements NoticeService {

    private static final String BIZ_TYPE_APPROVAL = "APPROVAL";
    private static final String BIZ_TYPE_TICKET = "TICKET";

    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private ApprovalFormMapper approvalFormMapper;
    @Resource
    private TicketMapper ticketMapper;

    @Override
    public long unreadCount(Long userId) {
        List<NoticeDO> unread = noticeMapper.selectList(new LambdaQueryWrapper<NoticeDO>()
                .eq(NoticeDO::getUserId, userId)
                .eq(NoticeDO::getReadStatus, 0));
        return unread.stream().filter(n -> !isOrphanNotice(n)).count();
    }

    @Override
    public List<NoticeDO> myList(Long userId) {
        List<NoticeDO> list = noticeMapper.selectList(new LambdaQueryWrapper<NoticeDO>()
                .eq(NoticeDO::getUserId, userId)
                .orderByDesc(NoticeDO::getCreateTime)
                .last("limit 50"));
        return list.stream().filter(n -> !isOrphanNotice(n)).toList();
    }

    @Override
    public void markRead(Long userId, Long noticeId) {
        NoticeDO notice = noticeMapper.selectById(noticeId);
        if (notice == null || !userId.equals(notice.getUserId())) {
            throw new BusinessException(404, "消息不存在");
        }
        notice.setReadStatus(1);
        noticeMapper.updateById(notice);
    }

    @Override
    public void deleteNotice(Long userId, Long noticeId) {
        NoticeDO notice = noticeMapper.selectById(noticeId);
        if (notice == null || !userId.equals(notice.getUserId())) {
            return;
        }
        noticeMapper.deleteById(noticeId);
    }

    @Override
    public void markAllRead(Long userId) {
        noticeMapper.markAllReadByUserId(userId);
    }

    @Override
    public void markReadByBiz(Long userId, String bizType, Long bizId) {
        if (userId == null || userId <= 0 || bizType == null || bizType.isEmpty() || bizId == null || bizId <= 0) {
            return;
        }
        noticeMapper.markReadByBiz(userId, bizType, bizId);
    }

    private boolean isOrphanNotice(NoticeDO notice) {
        if (notice.getBizId() == null || notice.getBizId() <= 0) {
            return false;
        }
        if (BIZ_TYPE_APPROVAL.equals(notice.getBizType())) {
            return approvalFormMapper.selectById(notice.getBizId()) == null;
        }
        if (BIZ_TYPE_TICKET.equals(notice.getBizType())) {
            return ticketMapper.selectById(notice.getBizId()) == null;
        }
        return false;
    }
}

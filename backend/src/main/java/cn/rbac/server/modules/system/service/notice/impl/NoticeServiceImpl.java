package cn.rbac.server.modules.system.service.notice.impl;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.modules.system.dal.dataobject.notice.NoticeDO;
import cn.rbac.server.modules.system.dal.mysql.approval.ApprovalFormMapper;
import cn.rbac.server.modules.system.dal.mysql.notice.NoticeMapper;
import cn.rbac.server.modules.system.service.notice.NoticeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeServiceImpl implements NoticeService {

    private static final String BIZ_TYPE_APPROVAL = "APPROVAL";

    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private ApprovalFormMapper approvalFormMapper;

    @Override
    public long unreadCount(Long userId) {
        List<NoticeDO> unread = noticeMapper.selectList(new LambdaQueryWrapper<NoticeDO>()
                .eq(NoticeDO::getUserId, userId)
                .eq(NoticeDO::getReadStatus, 0));
        return unread.stream().filter(n -> !isOrphanApprovalNotice(n)).count();
    }

    @Override
    public List<NoticeDO> myList(Long userId) {
        List<NoticeDO> list = noticeMapper.selectList(new LambdaQueryWrapper<NoticeDO>()
                .eq(NoticeDO::getUserId, userId)
                .orderByDesc(NoticeDO::getCreateTime)
                .last("limit 20"));
        return list.stream().filter(n -> !isOrphanApprovalNotice(n)).toList();
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

    private boolean isOrphanApprovalNotice(NoticeDO notice) {
        if (!BIZ_TYPE_APPROVAL.equals(notice.getBizType()) || notice.getBizId() == null || notice.getBizId() <= 0) {
            return false;
        }
        return approvalFormMapper.selectById(notice.getBizId()) == null;
    }
}

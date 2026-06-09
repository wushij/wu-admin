package cn.rbac.server.modules.system.service.notice.impl;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.modules.system.dal.dataobject.notice.NoticeDO;
import cn.rbac.server.modules.system.dal.mysql.notice.NoticeMapper;
import cn.rbac.server.modules.system.service.notice.NoticeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeServiceImpl implements NoticeService {

    @Resource
    private NoticeMapper noticeMapper;

    @Override
    public long unreadCount(Long userId) {
        return noticeMapper.selectCount(new LambdaQueryWrapper<NoticeDO>()
                .eq(NoticeDO::getUserId, userId)
                .eq(NoticeDO::getReadStatus, 0));
    }

    @Override
    public List<NoticeDO> myList(Long userId) {
        return noticeMapper.selectList(new LambdaQueryWrapper<NoticeDO>()
                .eq(NoticeDO::getUserId, userId)
                .orderByDesc(NoticeDO::getCreateTime)
                .last("limit 20"));
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
}

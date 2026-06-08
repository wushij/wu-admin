package cn.rbac.server.modules.system.service.message;

import cn.rbac.server.modules.system.dal.dataobject.notice.NoticeDO;
import cn.rbac.server.modules.system.dal.mysql.notice.NoticeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class NoticeService {

    @Resource
    private NoticeMapper noticeMapper;

    /** 用户未读站内消息数 */
    public long unreadCount(Long userId) {
        return noticeMapper.selectCount(new LambdaQueryWrapper<NoticeDO>()
                .eq(NoticeDO::getUserId, userId)
                .eq(NoticeDO::getReadStatus, 0));
    }
}

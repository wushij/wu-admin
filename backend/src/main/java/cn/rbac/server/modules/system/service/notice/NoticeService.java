package cn.rbac.server.modules.system.service.notice;

import cn.rbac.server.modules.system.dal.dataobject.notice.NoticeDO;

import java.util.List;

public interface NoticeService {

    long unreadCount(Long userId);

    List<NoticeDO> myList(Long userId);

    void markRead(Long userId, Long noticeId);

    void markAllRead(Long userId);
}

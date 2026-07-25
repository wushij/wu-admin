package com.admin.server.modules.system.service.notice;

import com.admin.server.modules.system.dal.dataobject.notice.NoticeDO;

import java.util.List;

public interface NoticeService {

    long unreadCount(Long userId);

    List<NoticeDO> myList(Long userId);

    void markRead(Long userId, Long noticeId);

    void markAllRead(Long userId);

    /** 将当前用户关联某业务的未读站内信标为已读 */
    void markReadByBiz(Long userId, String bizType, Long bizId);
}

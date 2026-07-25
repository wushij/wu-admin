package com.admin.server.modules.system.dal.dataobject.message;

import lombok.Data;

@Data
public class ChatPeerUnreadVO {
    private Long senderId;
    private Long unreadCount;
}

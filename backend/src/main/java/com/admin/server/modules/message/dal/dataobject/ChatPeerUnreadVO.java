package com.admin.server.modules.message.dal.dataobject;

import lombok.Data;

@Data
public class ChatPeerUnreadVO {
    private Long senderId;
    private Long unreadCount;
}

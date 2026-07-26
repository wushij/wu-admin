package com.admin.server.modules.message.dal.dataobject;

import lombok.Data;

@Data
public class ChatGroupMemberCountVO {
    private Long groupId;
    private Long memberCount;
}

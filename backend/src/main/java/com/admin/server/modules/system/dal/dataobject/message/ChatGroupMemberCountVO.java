package com.admin.server.modules.system.dal.dataobject.message;

import lombok.Data;

@Data
public class ChatGroupMemberCountVO {
    private Long groupId;
    private Long memberCount;
}

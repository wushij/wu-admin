package com.admin.server.modules.system.dal.dataobject.message;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatGroupLatestVO {
    private Long groupId;
    private String content;
    private Integer msgType;
    private LocalDateTime sendTime;
}

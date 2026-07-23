package com.admin.server.modules.system.dal.dataobject.message;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatPeerLatestVO {
    private Long peerId;
    private String content;
    private Integer msgType;
    private LocalDateTime sendTime;
}

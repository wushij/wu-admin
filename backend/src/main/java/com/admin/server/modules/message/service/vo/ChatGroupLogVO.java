package com.admin.server.modules.message.service.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatGroupLogVO {
    private Long id;
    private String actionType;
    private String content;
    private Long operatorId;
    private String operatorName;
    private Long targetUserId;
    private String targetUserName;
    private String detail;
    private LocalDateTime createTime;
}

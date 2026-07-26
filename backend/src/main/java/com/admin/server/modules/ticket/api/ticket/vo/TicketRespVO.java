package com.admin.server.modules.ticket.api.ticket.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TicketRespVO {
    private Long id;
    private String ticketNo;
    private String title;
    private String description;
    private String priority;
    private String status;
    private Long creatorUserId;
    private Long assigneeUserId;
    private String creatorName;
    private String assigneeName;
    private LocalDateTime deadline;
    private LocalDateTime closedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

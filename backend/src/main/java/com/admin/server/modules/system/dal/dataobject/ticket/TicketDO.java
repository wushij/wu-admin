package com.admin.server.modules.system.dal.dataobject.ticket;

import com.admin.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ticket")
public class TicketDO extends BaseEntity {

    private String ticketNo;

    private String title;

    private String description;

    /**
     * 优先级: LOW/MEDIUM/HIGH/URGENT
     */
    private String priority;

    /**
     * 状态: OPEN/IN_PROGRESS/RESOLVED/CLOSED
     */
    private String status;

    private Long creatorUserId;

    private Long assigneeUserId;

    private LocalDateTime deadline;

    private LocalDateTime closedTime;

    @TableField(exist = false)
    private String creatorName;

    @TableField(exist = false)
    private String assigneeName;
}

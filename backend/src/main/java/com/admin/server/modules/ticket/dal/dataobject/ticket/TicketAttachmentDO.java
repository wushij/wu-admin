package com.admin.server.modules.ticket.dal.dataobject.ticket;

import com.admin.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ticket_attachment")
public class TicketAttachmentDO extends BaseEntity {

    private Long ticketId;

    private Long uploaderUserId;

    private String fileName;

    private String filePath;

    private Long fileSize;

    @TableField(exist = false)
    private String uploaderName;
}

package cn.rbac.server.modules.system.dal.dataobject.ticket;

import cn.rbac.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ticket_comment")
public class TicketCommentDO extends BaseEntity {

    private Long ticketId;

    private Long userId;

    private String content;

    @TableField(exist = false)
    private String username;
}

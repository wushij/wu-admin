package com.admin.server.modules.message.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_chat_group_log")
public class ChatGroupLogDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    /** CREATE/INVITE/REMOVE/QUIT/DISSOLVE/UPDATE/SET_ADMIN/REMOVE_ADMIN/MUTE/UNMUTE/TRANSFER_OWNER */
    private String actionType;
    private Long operatorId;
    private String operatorName;
    private Long targetUserId;
    private String targetUserName;
    /** 补充说明，如批量邀请名单、修改后的群名 */
    private String detail;
    private LocalDateTime createTime;
}

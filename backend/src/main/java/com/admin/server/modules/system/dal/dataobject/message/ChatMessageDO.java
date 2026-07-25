package com.admin.server.modules.system.dal.dataobject.message;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_chat_message")
public class ChatMessageDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private Long receiverId;
    private String content;
    private Integer msgType;
    private Integer isRead;
    private LocalDateTime sendTime;
}

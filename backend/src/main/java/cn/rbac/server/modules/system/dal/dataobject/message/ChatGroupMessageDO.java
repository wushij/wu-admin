package cn.rbac.server.modules.system.dal.dataobject.message;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_chat_group_message")
public class ChatGroupMessageDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private Integer msgType;
    private LocalDateTime sendTime;
}

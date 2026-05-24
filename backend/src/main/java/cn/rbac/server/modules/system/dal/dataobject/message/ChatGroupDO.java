package cn.rbac.server.modules.system.dal.dataobject.message;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_chat_group")
public class ChatGroupDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String avatar;
    private Long ownerId;
    private String announcement;
    private Integer maxMembers;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

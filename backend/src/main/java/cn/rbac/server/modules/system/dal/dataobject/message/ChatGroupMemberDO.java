package cn.rbac.server.modules.system.dal.dataobject.message;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_chat_group_member")
public class ChatGroupMemberDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long userId;
    private String nickname;
    private Integer role;
    private Integer muted;
    private LocalDateTime joinTime;
}

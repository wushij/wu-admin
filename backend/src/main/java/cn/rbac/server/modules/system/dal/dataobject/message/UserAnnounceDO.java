package cn.rbac.server.modules.system.dal.dataobject.message;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_announce")
public class UserAnnounceDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long announceId;
    private Integer isRead;
    private LocalDateTime readTime;
    private LocalDateTime createTime;
}

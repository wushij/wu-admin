package cn.rbac.server.modules.system.dal.dataobject.message;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_announce_send_log")
public class AnnounceSendLogDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long announceId;
    private String channel;
    private Integer status;
    private Integer targetCount;
    private Integer successCount;
    private String errorMsg;
    private LocalDateTime sendTime;
}

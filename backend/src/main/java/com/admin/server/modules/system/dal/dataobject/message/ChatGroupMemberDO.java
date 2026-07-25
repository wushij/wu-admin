package com.admin.server.modules.system.dal.dataobject.message;

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
    /** 0 正常 1 被禁言 */
    private Integer muted;
    /** 0 正常 1 免打扰（仅 @ 我时提醒） */
    private Integer notifyMuted;
    /** 群公告已读时间（用于置顶公告「完成」） */
    private LocalDateTime announcementReadTime;
    private LocalDateTime joinTime;
}

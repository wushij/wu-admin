package cn.rbac.server.modules.system.dal.dataobject.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_user_post")
public class UserPostDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long postId;
}

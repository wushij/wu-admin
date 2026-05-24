package cn.rbac.server.modules.system.dal.dataobject.user;

import cn.rbac.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class UserDO extends BaseEntity {
    private String username;
    private String password;
    private String nickname;
    private String mobile;
    private String email;
    private String avatar;
    private Integer status;
    private Long deptId;
    
    /**
     * 部门名称（非数据库字段，用于展示）
     */
    @TableField(exist = false)
    private String deptName;
    
    /**
     * 角色ID列表（非数据库字段，用于展示）
     */
    @TableField(exist = false)
    private Set<Long> roleIds;
}

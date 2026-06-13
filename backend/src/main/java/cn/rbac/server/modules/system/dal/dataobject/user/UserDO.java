package cn.rbac.server.modules.system.dal.dataobject.user;

import cn.rbac.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class UserDO extends BaseEntity {
    private String username;
    @JsonIgnore
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

    @TableField(exist = false)
    private String postNames;

    @TableField(exist = false)
    private java.util.List<Long> postIds;

    /** 登录失败临时锁定（Redis，非 status 停用） */
    @TableField(exist = false)
    private Boolean loginLocked;

    /** 登录锁定剩余秒数 */
    @TableField(exist = false)
    private Long loginLockRemainSeconds;

    /** 登录失败累计次数（未锁定前） */
    @TableField(exist = false)
    private Integer loginFailCount;

    /** 最近登录 IP（登录日志，非数据库字段） */
    @TableField(exist = false)
    private String loginRecentIp;

    /** 最近登录 IP 是否临时锁定 */
    @TableField(exist = false)
    private Boolean loginIpLocked;

    /** 最近登录 IP 锁定剩余秒数 */
    @TableField(exist = false)
    private Long loginIpLockRemainSeconds;

    /** 最近登录 IP 失败累计次数 */
    @TableField(exist = false)
    private Integer loginIpFailCount;
}

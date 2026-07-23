package com.admin.server.modules.system.dal.dataobject.config;

import com.admin.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config_group")
public class SysConfigGroupDO extends BaseEntity {

    private String groupCode;
    private String groupName;
    private String configValue;
    private String remark;
}

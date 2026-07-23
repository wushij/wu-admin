package com.admin.server.modules.system.dal.dataobject.dict;

import com.admin.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class DictTypeDO extends BaseEntity {

    private String dictName;
    private String dictType;
    private Integer status;
    private String remark;

    @TableField(exist = false)
    private Long dataCount;
}

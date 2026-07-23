package com.admin.server.modules.system.dal.dataobject.message;

import com.admin.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_announce")
public class AnnounceDO extends BaseEntity {
    private String title;
    private String content;
    private Integer noticeType;
    private String channels;
    private Integer targetType;
    private String targetIds;
    private Integer status;
    private Long createBy;
    private String createName;
}

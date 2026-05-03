package cn.rbac.server.modules.system.dal.dataobject.notice;

import cn.rbac.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice")
public class NoticeDO extends BaseEntity {

    private Long userId;

    private String title;

    private String content;

    private String bizType;

    private Long bizId;

    /**
     * 0-未读 1-已读
     */
    private Integer readStatus;
}

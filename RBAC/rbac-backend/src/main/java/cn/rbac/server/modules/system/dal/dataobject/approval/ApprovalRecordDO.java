package cn.rbac.server.modules.system.dal.dataobject.approval;

import cn.rbac.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_approval_record")
public class ApprovalRecordDO extends BaseEntity {

    private Long formId;

    private Long operatorUserId;

    /**
     * SUBMIT/APPROVE/REJECT/ARCHIVE
     */
    private String action;

    private String remark;

    @TableField(exist = false)
    private String operatorName;
}

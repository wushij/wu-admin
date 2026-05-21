package cn.rbac.server.modules.system.dal.dataobject.approval;

import cn.rbac.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_approval_form")
public class ApprovalFormDO extends BaseEntity {

    private String formNo;

    /**
     * LEAVE/PURCHASE/REIMBURSE/SEAL/CONTRACT/GENERAL/REGISTER（用户注册审核）
     */
    private String formType;

    private String title;

    private String content;

    /**
     * SUBMITTED/APPROVED/REJECTED/ARCHIVED
     */
    private String status;

    private Long applicantUserId;

    private Long approverUserId;

    private String resultRemark;

    @TableField(exist = false)
    private String applicantName;

    @TableField(exist = false)
    private String approverName;
}

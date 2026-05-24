package com.admin.server.modules.ticket.service.approval;

import com.admin.server.modules.ticket.dal.dataobject.approval.ApprovalFormDO;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;

/**
 * 用户注册审核与审批单中心联动
 */
public interface RegisterApprovalService {

    String FORM_TYPE_REGISTER = "REGISTER";

    /**
     * 注册需审核时创建审批单并通知审批人
     */
    void createOnRegister(UserDO user);

    /**
     * 审批通过/驳回后同步注册用户状态
     */
    void applyApprovalResult(ApprovalFormDO form, String action);
}

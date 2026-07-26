package com.admin.server.modules.ticket.service.approval;

import com.admin.server.common.core.PageParam;
import com.admin.server.common.core.PageResult;
import com.admin.server.modules.ticket.api.approval.vo.ApprovalApproveReqVO;
import com.admin.server.modules.ticket.api.approval.vo.ApprovalArchiveReqVO;
import com.admin.server.modules.ticket.api.approval.vo.ApprovalApproverOptionVO;
import com.admin.server.modules.ticket.api.approval.vo.ApprovalCreateReqVO;
import com.admin.server.modules.ticket.dal.dataobject.approval.ApprovalFormDO;
import com.admin.server.modules.ticket.dal.dataobject.approval.ApprovalRecordDO;

import java.util.List;

public interface ApprovalFormService {

    PageResult<ApprovalFormDO> page(PageParam pageParam, String title, String formType, String status, Long userId);

    ApprovalFormDO get(Long id);

    List<ApprovalRecordDO> recordList(Long formId);

    List<ApprovalApproverOptionVO> listApproverOptions(Long currentUserId);

    Long create(ApprovalCreateReqVO reqVO, Long applicantUserId);

    void approve(ApprovalApproveReqVO reqVO, Long operatorUserId);

    void archive(ApprovalArchiveReqVO reqVO, Long operatorUserId);

    void delete(Long id);

    PageResult<ApprovalFormDO> recyclePage(PageParam pageParam, String title, String formType, String status);

    void restore(Long id);

    void deletePermanent(Long id);
}
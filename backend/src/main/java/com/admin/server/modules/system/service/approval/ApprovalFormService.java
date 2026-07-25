package com.admin.server.modules.system.service.approval;

import com.admin.server.common.pojo.PageParam;
import com.admin.server.common.pojo.PageResult;
import com.admin.server.modules.system.api.approval.vo.ApprovalApproveReqVO;
import com.admin.server.modules.system.api.approval.vo.ApprovalArchiveReqVO;
import com.admin.server.modules.system.api.approval.vo.ApprovalApproverOptionVO;
import com.admin.server.modules.system.api.approval.vo.ApprovalCreateReqVO;
import com.admin.server.modules.system.dal.dataobject.approval.ApprovalFormDO;
import com.admin.server.modules.system.dal.dataobject.approval.ApprovalRecordDO;

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
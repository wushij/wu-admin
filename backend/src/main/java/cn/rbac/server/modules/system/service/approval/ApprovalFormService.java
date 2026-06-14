package cn.rbac.server.modules.system.service.approval;

import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.api.approval.vo.ApprovalApproveReqVO;
import cn.rbac.server.modules.system.api.approval.vo.ApprovalArchiveReqVO;
import cn.rbac.server.modules.system.api.approval.vo.ApprovalApproverOptionVO;
import cn.rbac.server.modules.system.api.approval.vo.ApprovalCreateReqVO;
import cn.rbac.server.modules.system.dal.dataobject.approval.ApprovalFormDO;
import cn.rbac.server.modules.system.dal.dataobject.approval.ApprovalRecordDO;

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
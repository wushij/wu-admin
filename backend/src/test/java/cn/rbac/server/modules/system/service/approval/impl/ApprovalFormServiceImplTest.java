package cn.rbac.server.modules.system.service.approval.impl;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.modules.system.api.approval.vo.ApprovalApproveReqVO;
import cn.rbac.server.modules.system.api.approval.vo.ApprovalArchiveReqVO;
import cn.rbac.server.modules.system.api.approval.vo.ApprovalCreateReqVO;
import cn.rbac.server.modules.system.dal.dataobject.approval.ApprovalFormDO;
import cn.rbac.server.modules.system.dal.dataobject.approval.ApprovalRecordDO;
import cn.rbac.server.modules.system.dal.mysql.approval.ApprovalFormMapper;
import cn.rbac.server.modules.system.dal.mysql.approval.ApprovalRecordMapper;
import cn.rbac.server.modules.system.dal.mysql.notice.NoticeMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.service.approval.RegisterApprovalService;
import cn.rbac.server.modules.system.service.permission.PermissionService;
import cn.rbac.server.testsupport.MybatisLambdaTestBase;
import cn.rbac.server.testsupport.ServiceTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalFormServiceImpl 单元测试")
class ApprovalFormServiceImplTest extends MybatisLambdaTestBase {

    @Mock
    private ApprovalFormMapper approvalFormMapper;
    @Mock
    private ApprovalRecordMapper approvalRecordMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private NoticeMapper noticeMapper;
    @Mock
    private PermissionService permissionService;
    @Mock
    private RegisterApprovalService registerApprovalService;

    @InjectMocks
    private ApprovalFormServiceImpl approvalFormService;

    @Test
    @DisplayName("get：审批单不存在抛 404")
    void get_notFound() {
        when(approvalFormMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalFormService.get(1L));

        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("create：未选择审批人抛 400")
    void create_missingApprover() {
        ApprovalCreateReqVO req = new ApprovalCreateReqVO();
        req.setTitle("请假");
        req.setApproverUserId(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalFormService.create(req, 10L));

        assertEquals(400, ex.getCode());
        verify(approvalFormMapper, never()).insert(any(ApprovalFormDO.class));
    }

    @Test
    @DisplayName("create：不能选择自己作为审批人")
    void create_selfApproverForbidden() {
        ApprovalCreateReqVO req = new ApprovalCreateReqVO();
        req.setTitle("请假");
        req.setApproverUserId(10L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalFormService.create(req, 10L));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不能选择自己"));
        verify(approvalFormMapper, never()).insert(any(ApprovalFormDO.class));
    }

    @Test
    @DisplayName("approve：非审批人且无特权时拒绝")
    void approve_forbiddenForOthers() {
        ApprovalFormDO form = ServiceTestFixtures.generalApprovalForm(1L, 10L, 99L, "SUBMITTED");
        when(approvalFormMapper.selectById(1L)).thenReturn(form);
        when(permissionService.hasRole(20L, "super_admin")).thenReturn(false);

        ApprovalApproveReqVO req = new ApprovalApproveReqVO();
        req.setId(1L);
        req.setAction("APPROVE");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalFormService.approve(req, 20L));

        assertEquals(403, ex.getCode());
        verify(approvalFormMapper, never()).updateById(any(ApprovalFormDO.class));
    }

    @Test
    @DisplayName("approve：非待审状态不可审批")
    void approve_invalidStatus() {
        ApprovalFormDO form = ServiceTestFixtures.generalApprovalForm(1L, 10L, 20L, "APPROVED");
        when(approvalFormMapper.selectById(1L)).thenReturn(form);

        ApprovalApproveReqVO req = new ApprovalApproveReqVO();
        req.setId(1L);
        req.setAction("APPROVE");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalFormService.approve(req, 20L));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("待审批"));
    }

    @Test
    @DisplayName("approve：审批人可通过审批")
    void approve_byAssignee() {
        ApprovalFormDO form = ServiceTestFixtures.generalApprovalForm(1L, 10L, 20L, "SUBMITTED");
        when(approvalFormMapper.selectById(1L)).thenReturn(form);
        when(permissionService.hasRole(20L, "super_admin")).thenReturn(false);

        ApprovalApproveReqVO req = new ApprovalApproveReqVO();
        req.setId(1L);
        req.setAction("APPROVE");
        req.setRemark("同意");

        approvalFormService.approve(req, 20L);

        verify(approvalFormMapper).updateById(argThat((ApprovalFormDO f) -> "APPROVED".equals(f.getStatus())));
        verify(registerApprovalService).applyApprovalResult(form, "APPROVE");
        verify(approvalRecordMapper).insert(any(ApprovalRecordDO.class));
    }

    @Test
    @DisplayName("archive：非申请人归档时拒绝")
    void archive_forbiddenForOthers() {
        ApprovalFormDO form = ServiceTestFixtures.generalApprovalForm(1L, 10L, 99L, "APPROVED");
        when(approvalFormMapper.selectById(1L)).thenReturn(form);
        when(permissionService.hasRole(20L, "super_admin")).thenReturn(false);

        ApprovalArchiveReqVO req = new ApprovalArchiveReqVO();
        req.setId(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalFormService.archive(req, 20L));

        assertEquals(403, ex.getCode());
        verify(approvalFormMapper, never()).updateById(any(ApprovalFormDO.class));
    }

    @Test
    @DisplayName("archive：申请人可归档")
    void archive_byApplicant() {
        ApprovalFormDO form = ServiceTestFixtures.generalApprovalForm(1L, 10L, 99L, "APPROVED");
        when(approvalFormMapper.selectById(1L)).thenReturn(form);

        ApprovalArchiveReqVO req = new ApprovalArchiveReqVO();
        req.setId(1L);
        req.setRemark("完成");

        approvalFormService.archive(req, 10L);

        verify(approvalFormMapper).updateById(argThat((ApprovalFormDO f) -> "ARCHIVED".equals(f.getStatus())));
        verify(approvalRecordMapper).insert(any(ApprovalRecordDO.class));
    }

    @Test
    @DisplayName("delete：同步删除关联站内信")
    void delete_removesRelatedNotices() {
        ApprovalFormDO form = ServiceTestFixtures.generalApprovalForm(4L, 10L, 99L, "ARCHIVED");
        when(approvalFormMapper.selectById(4L)).thenReturn(form);

        approvalFormService.delete(4L);

        verify(noticeMapper).delete(any());
        verify(approvalRecordMapper).delete(any());
        verify(approvalFormMapper).deleteById(4L);
    }

    @Test
    @DisplayName("deletePermanent：物理删除关联站内信")
    void deletePermanent_removesRelatedNotices() {
        when(approvalFormMapper.deletePhysicalById(4L)).thenReturn(1);

        approvalFormService.deletePermanent(4L);

        verify(noticeMapper).deletePhysicalByBiz("APPROVAL", 4L);
        verify(approvalRecordMapper).deletePhysicalByFormId(4L);
        verify(approvalFormMapper).deletePhysicalById(4L);
    }
}

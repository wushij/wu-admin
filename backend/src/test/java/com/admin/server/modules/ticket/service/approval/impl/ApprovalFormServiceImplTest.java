package com.admin.server.modules.ticket.service.approval.impl;

import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.ticket.api.approval.vo.ApprovalApproveReqVO;
import com.admin.server.modules.ticket.api.approval.vo.ApprovalArchiveReqVO;
import com.admin.server.modules.ticket.api.approval.vo.ApprovalCreateReqVO;
import com.admin.server.modules.ticket.dal.dataobject.approval.ApprovalFormDO;
import com.admin.server.modules.ticket.dal.dataobject.approval.ApprovalRecordDO;
import com.admin.server.modules.ticket.dal.mysql.approval.ApprovalFormMapper;
import com.admin.server.modules.ticket.dal.mysql.approval.ApprovalRecordMapper;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;
import com.admin.server.modules.ticket.service.approval.RegisterApprovalService;
import com.admin.server.modules.system.service.notice.NoticeService;
import com.admin.server.modules.system.service.permission.DataScopeService;
import com.admin.server.modules.system.service.permission.PermissionService;
import com.admin.server.testsupport.MybatisLambdaTestBase;
import com.admin.server.testsupport.ServiceTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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
    private PermissionService permissionService;
    @Mock
    private RegisterApprovalService registerApprovalService;
    @Mock
    private NoticeService noticeService;
    @Mock
    private DataScopeService dataScopeService;
    @Mock
    private com.admin.server.modules.system.service.config.SystemConfigHelper systemConfigHelper;
    @Mock
    private com.admin.server.modules.system.service.user.UserService userService;

    @InjectMocks
    private ApprovalFormServiceImpl approvalFormService;

    @BeforeEach
    void stubDataScope() {
        // 审批单可见性默认按超管放行，具体用例可覆盖
        lenient().when(dataScopeService.getCurrentScope())
                .thenReturn(new DataScopeService.DataScopeContext(true, false, List.of(), 1L, 1L));
    }

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

        verify(noticeService).deleteNoticeByBiz("APPROVAL", 4L, false);
        verify(approvalRecordMapper).delete(any());
        verify(approvalFormMapper).deleteById(4L);
    }

    @Test
    @DisplayName("deletePermanent：物理删除关联站内信")
    void deletePermanent_removesRelatedNotices() {
        ApprovalFormDO form = ServiceTestFixtures.generalApprovalForm(4L, 10L, 99L, "ARCHIVED");
        when(approvalFormMapper.selectDeletedById(4L)).thenReturn(form);
        when(approvalFormMapper.deletePhysicalById(4L)).thenReturn(1);

        approvalFormService.deletePermanent(4L);

        verify(noticeService).deleteNoticeByBiz("APPROVAL", 4L, true);
        verify(approvalRecordMapper).deletePhysicalByFormId(4L);
        verify(approvalFormMapper).deletePhysicalById(4L);
    }

    @Test
    @DisplayName("get：普通用户受限于数据范围时，普通业务单据不可见抛 403")
    void get_generalForm_forbiddenForLimitedDataScope() {
        ApprovalFormDO form = ServiceTestFixtures.generalApprovalForm(1L, 10L, 99L, "SUBMITTED");
        when(approvalFormMapper.selectById(1L)).thenReturn(form);
        when(dataScopeService.getCurrentScope())
                .thenReturn(new DataScopeService.DataScopeContext(false, true, List.of(), 5L, null));
        try (org.mockito.MockedStatic<com.admin.server.framework.security.core.service.SecurityUtils> mocked =
                     mockStatic(com.admin.server.framework.security.core.service.SecurityUtils.class)) {
            mocked.when(com.admin.server.framework.security.core.service.SecurityUtils::getLoginUserId).thenReturn(5L);

            BusinessException ex = assertThrows(BusinessException.class, () -> approvalFormService.get(1L));
            assertEquals(403, ex.getCode());
        }
    }

    @Test
    @DisplayName("get：普通用户属于配置的注册审核人时，即使数据范围受限也能查看注册审批单")
    void get_registerForm_visibleForConfiguredAuditor() {
        ApprovalFormDO form = new ApprovalFormDO();
        form.setId(1L);
        form.setFormType(RegisterApprovalService.FORM_TYPE_REGISTER);
        form.setApplicantUserId(88L); // 新注册用户
        form.setApproverUserId(1L);   // 超管作为默认主审批人
        form.setStatus("SUBMITTED");
        when(approvalFormMapper.selectById(1L)).thenReturn(form);
        when(dataScopeService.getCurrentScope())
                .thenReturn(new DataScopeService.DataScopeContext(false, true, List.of(), 5L, null));
        when(permissionService.hasRole(5L, "super_admin")).thenReturn(false);
        when(systemConfigHelper.getRegisterAuditorUserIds()).thenReturn(List.of(5L)); // 5L 为配置的注册审核人

        try (org.mockito.MockedStatic<com.admin.server.framework.security.core.service.SecurityUtils> mocked =
                     mockStatic(com.admin.server.framework.security.core.service.SecurityUtils.class)) {
            mocked.when(com.admin.server.framework.security.core.service.SecurityUtils::getLoginUserId).thenReturn(5L);

            ApprovalFormDO result = approvalFormService.get(1L);
            assertNotNull(result);
            assertEquals(1L, result.getId());
        }
    }

    @Test
    @DisplayName("get：普通用户拥有审批权限时，即使数据范围受限也能查看注册审批单")
    void get_registerForm_visibleForApprovalPermissionUser() {
        ApprovalFormDO form = new ApprovalFormDO();
        form.setId(1L);
        form.setFormType(RegisterApprovalService.FORM_TYPE_REGISTER);
        form.setApplicantUserId(88L);
        form.setApproverUserId(1L);
        form.setStatus("SUBMITTED");
        when(approvalFormMapper.selectById(1L)).thenReturn(form);
        when(dataScopeService.getCurrentScope())
                .thenReturn(new DataScopeService.DataScopeContext(false, true, List.of(), 5L, null));
        when(permissionService.hasRole(5L, "super_admin")).thenReturn(false);
        when(systemConfigHelper.getRegisterAuditorUserIds()).thenReturn(List.of());
        when(permissionService.hasPermission(5L, "system:approval:approve")).thenReturn(true);

        try (org.mockito.MockedStatic<com.admin.server.framework.security.core.service.SecurityUtils> mocked =
                     mockStatic(com.admin.server.framework.security.core.service.SecurityUtils.class)) {
            mocked.when(com.admin.server.framework.security.core.service.SecurityUtils::getLoginUserId).thenReturn(5L);

            ApprovalFormDO result = approvalFormService.get(1L);
            assertNotNull(result);
            assertEquals(1L, result.getId());
        }
    }
}

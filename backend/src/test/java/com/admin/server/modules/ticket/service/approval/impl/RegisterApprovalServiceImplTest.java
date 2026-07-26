package com.admin.server.modules.ticket.service.approval.impl;

import com.admin.server.modules.ticket.dal.dataobject.approval.ApprovalFormDO;
import com.admin.server.modules.ticket.dal.dataobject.approval.ApprovalRecordDO;
import com.admin.server.modules.system.dal.dataobject.notice.NoticeDO;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;
import com.admin.server.modules.ticket.dal.mysql.approval.ApprovalFormMapper;
import com.admin.server.modules.ticket.dal.mysql.approval.ApprovalRecordMapper;
import com.admin.server.modules.system.dal.mysql.notice.NoticeMapper;
import com.admin.server.modules.system.dal.mysql.permission.RoleMapper;
import com.admin.server.modules.system.dal.mysql.permission.UserRoleMapper;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;
import com.admin.server.modules.system.dal.mysql.user.UserPostMapper;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.admin.server.testsupport.MybatisLambdaTestBase;
import com.admin.server.testsupport.MybatisMockMatchers;
import com.admin.server.testsupport.ServiceTestFixtures;
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
@DisplayName("RegisterApprovalServiceImpl 单元测试")
class RegisterApprovalServiceImplTest extends MybatisLambdaTestBase {

    @Mock
    private ApprovalFormMapper approvalFormMapper;
    @Mock
    private ApprovalRecordMapper approvalRecordMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private UserPostMapper userPostMapper;
    @Mock
    private NoticeMapper noticeMapper;
    @Mock
    private SystemConfigHelper systemConfigHelper;

    @InjectMocks
    private RegisterApprovalServiceImpl registerApprovalService;

    @Test
    @DisplayName("parseRegisterUserId：解析 JSON 中的 userId")
    void parseRegisterUserId_fromJson() {
        String content = "{\"bizType\":\"USER_REGISTER\",\"userId\":42,\"username\":\"bob\"}";
        assertEquals(42L, RegisterApprovalServiceImpl.parseRegisterUserId(content));
    }

    @Test
    @DisplayName("parseRegisterUserId：空内容返回 null")
    void parseRegisterUserId_blank() {
        assertNull(RegisterApprovalServiceImpl.parseRegisterUserId(null));
        assertNull(RegisterApprovalServiceImpl.parseRegisterUserId("  "));
    }

    @Test
    @DisplayName("applyApprovalResult：审批通过激活用户")
    void applyApprovalResult_approve() {
        UserDO user = ServiceTestFixtures.user(10L, "pending", 2);
        ApprovalFormDO form = ServiceTestFixtures.approvalForm(1L, 10L, "SUBMITTED");
        when(userMapper.selectById(10L)).thenReturn(user);

        registerApprovalService.applyApprovalResult(form, "APPROVE");

        verify(userMapper).updateById(argThat((UserDO u) -> u.getStatus() == 1));
        verify(userMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("applyApprovalResult：审批拒绝删除用户及关联")
    void applyApprovalResult_reject() {
        UserDO user = ServiceTestFixtures.user(10L, "pending", 2);
        ApprovalFormDO form = ServiceTestFixtures.approvalForm(1L, 10L, "SUBMITTED");
        when(userMapper.selectById(10L)).thenReturn(user);

        registerApprovalService.applyApprovalResult(form, "REJECT");

        verify(userRoleMapper).deleteByUserId(10L);
        verify(userPostMapper).deleteByUserId(10L);
        verify(userMapper).deleteById(10L);
    }

    @Test
    @DisplayName("createOnRegister：已有待审表单时跳过")
    void createOnRegister_skipsWhenPending() {
        UserDO user = ServiceTestFixtures.user(5L, "newbie", 2);
        when(approvalFormMapper.selectCount(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(1L);

        registerApprovalService.createOnRegister(user);

        verify(approvalFormMapper, never()).insert(any(ApprovalFormDO.class));
    }

    @Test
    @DisplayName("createOnRegister：创建审批单并通知管理员")
    void createOnRegister_createsForm() {
        UserDO user = ServiceTestFixtures.user(5L, "newbie", 2);
        when(approvalFormMapper.selectCount(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(0L);
        when(systemConfigHelper.getRegisterAuditorUserIds()).thenReturn(List.of());
        when(roleMapper.selectOne(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(null);

        doAnswer(inv -> {
            ApprovalFormDO form = inv.getArgument(0);
            form.setId(100L);
            return 1;
        }).when(approvalFormMapper).insert(any(ApprovalFormDO.class));

        registerApprovalService.createOnRegister(user);

        verify(approvalFormMapper).insert(argThat((ApprovalFormDO f) ->
                "REGISTER".equals(f.getFormType()) && f.getApplicantUserId() == 5L));
        verify(approvalRecordMapper).insert(any(ApprovalRecordDO.class));
        verify(noticeMapper).insert(any(NoticeDO.class));
    }

    @Test
    @DisplayName("displayNameFromContent：用户删除后仍可从 JSON 解析申请人")
    void displayNameFromContent_readsNicknameOrUsername() {
        String content = "{\"bizType\":\"USER_REGISTER\",\"userId\":5,\"username\":\"admin1\",\"nickname\":\"11\"}";
        assertEquals("11", RegisterApprovalServiceImpl.displayNameFromContent(content));
        assertEquals("admin1", RegisterApprovalServiceImpl.displayNameFromContent(
                "{\"bizType\":\"USER_REGISTER\",\"userId\":5,\"username\":\"admin1\"}"));
        assertNull(RegisterApprovalServiceImpl.displayNameFromContent("{\"bizType\":\"OTHER\"}"));
    }
}

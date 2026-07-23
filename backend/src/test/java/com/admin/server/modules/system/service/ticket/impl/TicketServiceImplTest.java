package com.admin.server.modules.system.service.ticket.impl;

import com.admin.server.common.pojo.BusinessException;
import com.admin.server.modules.system.api.ticket.vo.TicketCommentCreateReqVO;
import com.admin.server.modules.system.api.ticket.vo.TicketCreateReqVO;
import com.admin.server.modules.system.api.ticket.vo.TicketTransitionReqVO;
import com.admin.server.modules.system.dal.dataobject.notice.NoticeDO;
import com.admin.server.modules.system.dal.dataobject.ticket.TicketCommentDO;
import com.admin.server.modules.system.dal.dataobject.ticket.TicketDO;
import com.admin.server.modules.system.dal.mysql.notice.NoticeMapper;
import com.admin.server.modules.system.dal.mysql.ticket.TicketAttachmentMapper;
import com.admin.server.modules.system.dal.mysql.ticket.TicketCommentMapper;
import com.admin.server.modules.system.dal.mysql.ticket.TicketMapper;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;
import com.admin.server.modules.system.service.notice.NoticeService;
import com.admin.server.modules.system.service.permission.PermissionService;
import com.admin.server.testsupport.ServiceTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketServiceImpl 单元测试")
class TicketServiceImplTest {

    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private TicketCommentMapper ticketCommentMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private NoticeMapper noticeMapper;
    @Mock
    private TicketAttachmentMapper ticketAttachmentMapper;
    @Mock
    private PermissionService permissionService;
    @Mock
    private NoticeService noticeService;

    @InjectMocks
    private TicketServiceImpl ticketService;

    @Test
    @DisplayName("create：不能选择自己作为处理人")
    void create_rejectsSelfAssignee() {
        TicketCreateReqVO req = new TicketCreateReqVO();
        req.setTitle("自助工单");
        req.setAssigneeUserId(10L);

        BusinessException ex = assertThrows(BusinessException.class, () -> ticketService.create(req, 10L));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不能选择自己"));
        verify(ticketMapper, never()).insert(any(TicketDO.class));
    }

    @Test
    @DisplayName("create：创建工单并通知处理人")
    void create_assignsAndNotifies() {
        TicketCreateReqVO req = new TicketCreateReqVO();
        req.setTitle("网络故障");
        req.setDescription("无法上网");
        req.setAssigneeUserId(20L);

        doAnswer(inv -> {
            TicketDO ticket = inv.getArgument(0);
            ticket.setId(100L);
            return 1;
        }).when(ticketMapper).insert(any(TicketDO.class));

        Long id = ticketService.create(req, 10L);

        assertEquals(100L, id);
        verify(ticketMapper).insert(argThat((TicketDO t) ->
                "OPEN".equals(t.getStatus()) && t.getCreatorUserId() == 10L && t.getAssigneeUserId() == 20L));
        verify(noticeMapper).insert(any(NoticeDO.class));
    }

    @Test
    @DisplayName("createComment：工单不存在抛 404")
    void createComment_ticketNotFound() {
        when(ticketMapper.selectById(1L)).thenReturn(null);
        TicketCommentCreateReqVO req = new TicketCommentCreateReqVO();
        req.setTicketId(1L);
        req.setContent("跟进中");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ticketService.createComment(req, 10L));

        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("createComment：成功添加评论")
    void createComment_success() {
        when(ticketMapper.selectById(1L)).thenReturn(ServiceTestFixtures.ticket(1L, 10L, "OPEN"));
        doAnswer(inv -> {
            TicketCommentDO comment = inv.getArgument(0);
            comment.setId(50L);
            return 1;
        }).when(ticketCommentMapper).insert(any(TicketCommentDO.class));

        TicketCommentCreateReqVO req = new TicketCommentCreateReqVO();
        req.setTicketId(1L);
        req.setContent("  已联系用户  ");

        Long commentId = ticketService.createComment(req, 10L);

        assertEquals(50L, commentId);
        verify(ticketCommentMapper).insert(argThat((TicketCommentDO c) ->
                "已联系用户".equals(c.getContent()) && c.getUserId() == 10L));
    }

    @Test
    @DisplayName("transition：工单不存在抛 404")
    void transition_ticketNotFound() {
        when(ticketMapper.selectById(1L)).thenReturn(null);
        TicketTransitionReqVO req = transitionReq(1L, "IN_PROGRESS");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ticketService.transition(req, 10L));

        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("transition：非负责人且无流转权限时拒绝")
    void transition_forbiddenForOthers() {
        TicketDO ticket = ServiceTestFixtures.ticket(1L, 99L, "OPEN");
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(permissionService.hasRole(10L, "super_admin")).thenReturn(false);
        when(permissionService.hasPermission(10L, "system:ticket:transition")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ticketService.transition(transitionReq(1L, "IN_PROGRESS"), 10L));

        assertEquals(403, ex.getCode());
        verify(ticketMapper, never()).updateById(any(TicketDO.class));
    }

    @Test
    @DisplayName("transition：非法状态抛 400")
    void transition_invalidStatus() {
        TicketDO ticket = ServiceTestFixtures.ticket(1L, 10L, "OPEN");
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ticketService.transition(transitionReq(1L, "UNKNOWN"), 10L));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("状态"));
    }

    @Test
    @DisplayName("transition：负责人可流转到处理中")
    void transition_assigneeCanProgress() {
        TicketDO ticket = ServiceTestFixtures.ticket(1L, 10L, "OPEN");
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        ticketService.transition(transitionReq(1L, "IN_PROGRESS"), 10L);

        ArgumentCaptor<TicketDO> captor = ArgumentCaptor.forClass(TicketDO.class);
        verify(ticketMapper).updateById(captor.capture());
        assertEquals("IN_PROGRESS", captor.getValue().getStatus());
        assertNull(captor.getValue().getClosedTime());
        verify(noticeService).markReadByBiz(10L, "TICKET", 1L);
    }

    @Test
    @DisplayName("transition：关闭工单时写入 closedTime")
    void transition_closedSetsClosedTime() {
        TicketDO ticket = ServiceTestFixtures.ticket(1L, 10L, "RESOLVED");
        when(ticketMapper.selectById(1L)).thenReturn(ticket);

        ticketService.transition(transitionReq(1L, "CLOSED"), 10L);

        ArgumentCaptor<TicketDO> captor = ArgumentCaptor.forClass(TicketDO.class);
        verify(ticketMapper).updateById(captor.capture());
        assertEquals("CLOSED", captor.getValue().getStatus());
        assertNotNull(captor.getValue().getClosedTime());
    }

    private static TicketTransitionReqVO transitionReq(long id, String status) {
        TicketTransitionReqVO req = new TicketTransitionReqVO();
        req.setId(id);
        req.setStatus(status);
        return req;
    }
}

package cn.rbac.server.modules.system.service.notice.impl;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.modules.system.dal.dataobject.notice.NoticeDO;
import cn.rbac.server.modules.system.dal.mysql.approval.ApprovalFormMapper;
import cn.rbac.server.modules.system.dal.mysql.notice.NoticeMapper;
import cn.rbac.server.testsupport.MybatisLambdaTestBase;
import cn.rbac.server.testsupport.MybatisMockMatchers;
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
@DisplayName("NoticeServiceImpl 单元测试")
class NoticeServiceImplTest extends MybatisLambdaTestBase {

    private static final long USER_ID = 1L;

    @Mock
    private NoticeMapper noticeMapper;
    @Mock
    private ApprovalFormMapper approvalFormMapper;

    @InjectMocks
    private NoticeServiceImpl noticeService;

    @Test
    @DisplayName("unreadCount：返回未读数量")
    void unreadCount_returnsCount() {
        NoticeDO notice = new NoticeDO();
        notice.setId(1L);
        notice.setReadStatus(0);
        notice.setTitle("通知");
        when(noticeMapper.selectList(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(List.of(notice));

        assertEquals(1L, noticeService.unreadCount(USER_ID));
    }

    @Test
    @DisplayName("unreadCount：过滤审批单已删的孤立消息")
    void unreadCount_filtersOrphanApprovalNotice() {
        NoticeDO orphan = new NoticeDO();
        orphan.setId(1L);
        orphan.setReadStatus(0);
        orphan.setBizType("APPROVAL");
        orphan.setBizId(99L);
        when(noticeMapper.selectList(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(List.of(orphan));
        when(approvalFormMapper.selectById(99L)).thenReturn(null);

        assertEquals(0L, noticeService.unreadCount(USER_ID));
    }

    @Test
    @DisplayName("myList：返回当前用户消息")
    void myList_returnsUserNotices() {
        NoticeDO notice = new NoticeDO();
        notice.setId(1L);
        notice.setUserId(USER_ID);
        notice.setTitle("系统通知");
        when(noticeMapper.selectList(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(List.of(notice));

        List<NoticeDO> list = noticeService.myList(USER_ID);

        assertEquals(1, list.size());
        assertEquals("系统通知", list.get(0).getTitle());
    }

    @Test
    @DisplayName("myList：过滤审批单已删的孤立消息")
    void myList_filtersOrphanApprovalNotice() {
        NoticeDO orphan = new NoticeDO();
        orphan.setId(1L);
        orphan.setUserId(USER_ID);
        orphan.setTitle("注册待审核");
        orphan.setBizType("APPROVAL");
        orphan.setBizId(99L);
        when(noticeMapper.selectList(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(List.of(orphan));
        when(approvalFormMapper.selectById(99L)).thenReturn(null);

        assertTrue(noticeService.myList(USER_ID).isEmpty());
    }

    @Test
    @DisplayName("markRead：非本人消息抛 404")
    void markRead_notOwner() {
        NoticeDO notice = new NoticeDO();
        notice.setId(1L);
        notice.setUserId(99L);
        when(noticeMapper.selectById(1L)).thenReturn(notice);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> noticeService.markRead(USER_ID, 1L));

        assertEquals(404, ex.getCode());
        verify(noticeMapper, never()).updateById(any(NoticeDO.class));
    }

    @Test
    @DisplayName("markRead：标记已读")
    void markRead_success() {
        NoticeDO notice = new NoticeDO();
        notice.setId(1L);
        notice.setUserId(USER_ID);
        notice.setReadStatus(0);
        when(noticeMapper.selectById(1L)).thenReturn(notice);

        noticeService.markRead(USER_ID, 1L);

        verify(noticeMapper).updateById(argThat((NoticeDO n) -> n.getReadStatus() == 1));
    }

    @Test
    @DisplayName("markAllRead：批量标记已读")
    void markAllRead_delegatesToMapper() {
        noticeService.markAllRead(USER_ID);

        verify(noticeMapper).markAllReadByUserId(USER_ID);
    }
}

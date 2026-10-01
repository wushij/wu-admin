package com.admin.server.modules.message.service;

import com.admin.server.common.exception.BusinessException;
import com.admin.server.framework.websocket.MessageWebSocketHandler;
import com.admin.server.modules.message.dal.dataobject.AnnounceDO;
import com.admin.server.modules.message.dal.mysql.AnnounceMapper;
import com.admin.server.modules.message.dal.mysql.AnnounceSendLogMapper;
import com.admin.server.modules.message.dal.mysql.UserAnnounceMapper;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;
import com.admin.server.modules.system.service.permission.PermissionService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 系统通知可见性单元测试：确保非通知管理用户无法通过 id 枚举读到草稿或他人的定向公告。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnnounceService 通知可见性测试")
class AnnounceServiceVisibilityTest {

    /** 普通登录用户（体验账号同款：只有 announce 的 list/query 只读权限） */
    private static final long USER_ID = 10L;
    /** 他人（公告发布者） */
    private static final long AUTHOR_ID = 99L;
    /** 已发布 */
    private static final int STATUS_PUBLISHED = 1;
    /** 草稿 */
    private static final int STATUS_DRAFT = 0;

    @Mock
    private AnnounceMapper announceMapper;
    @Mock
    private UserAnnounceMapper userAnnounceMapper;
    @Mock
    private AnnounceSendLogMapper sendLogMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private MessageWebSocketHandler webSocketHandler;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private AnnounceService announceService;

    @Test
    @DisplayName("detail：通知不存在抛 404")
    void detail_notFound() {
        when(announceMapper.selectById(5L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> announceService.getVisibleById(5L, USER_ID));

        assertEquals(404, ex.getCode());
        verifyNoInteractions(permissionService);
    }

    @Test
    @DisplayName("detail：非管理用户读草稿被拒 403")
    void detail_draftDeniedForNormalUser() {
        when(announceMapper.selectById(5L)).thenReturn(announce(5L, STATUS_DRAFT, AUTHOR_ID));
        stubNotManager();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> announceService.getVisibleById(5L, USER_ID));

        assertEquals(403, ex.getCode());
        // 草稿在状态判定阶段即被拦截，不应再去查投递关系
        verifyNoInteractions(userAnnounceMapper);
    }

    @Test
    @DisplayName("detail：非管理用户读已发布但未投递给自己的通知被拒 403")
    void detail_publishedButNotDeliveredDenied() {
        when(announceMapper.selectById(5L)).thenReturn(announce(5L, STATUS_PUBLISHED, AUTHOR_ID));
        stubNotManager();
        when(userAnnounceMapper.selectAnnounceIdsByUserId(USER_ID)).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> announceService.getVisibleById(5L, USER_ID));

        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("detail：非管理用户读已投递的已发布通知放行")
    void detail_deliveredPublishedAllowed() {
        when(announceMapper.selectById(5L)).thenReturn(announce(5L, STATUS_PUBLISHED, AUTHOR_ID));
        stubNotManager();
        when(userAnnounceMapper.selectAnnounceIdsByUserId(USER_ID)).thenReturn(List.of(5L));

        assertEquals(5L, announceService.getVisibleById(5L, USER_ID).getId());
    }

    @Test
    @DisplayName("detail：发布人可查看自己的草稿")
    void detail_authorCanReadOwnDraft() {
        when(announceMapper.selectById(5L)).thenReturn(announce(5L, STATUS_DRAFT, USER_ID));
        stubNotManager();

        assertEquals(5L, announceService.getVisibleById(5L, USER_ID).getId());
        verifyNoInteractions(userAnnounceMapper);
    }

    @Test
    @DisplayName("detail：通知管理用户可查看草稿")
    void detail_managerCanReadDraft() {
        when(announceMapper.selectById(5L)).thenReturn(announce(5L, STATUS_DRAFT, AUTHOR_ID));
        when(permissionService.hasRole(USER_ID, "super_admin")).thenReturn(false);
        when(permissionService.hasPermission(USER_ID, "system:announce:create")).thenReturn(true);

        assertEquals(5L, announceService.getVisibleById(5L, USER_ID).getId());
        verifyNoInteractions(userAnnounceMapper);
    }

    @Test
    @DisplayName("page：非管理用户无可见通知时返回空页且不查库")
    void page_returnsEmptyWhenNoVisibleAnnounce() {
        stubNotManager();
        when(userAnnounceMapper.selectAnnounceIdsByUserId(USER_ID)).thenReturn(List.of());

        Page<AnnounceDO> page = announceService.page(1, 10, null, null, null, USER_ID);

        assertEquals(0L, page.getTotal());
        assertTrue(page.getRecords().isEmpty());
        // 无可见范围时不应退化成「查全表」，否则草稿仍会随分页泄露
        verify(announceMapper, never()).selectPage(any(), any());
    }

    @Test
    @DisplayName("send-logs：非管理且非发布人读取被拒 403")
    void sendLogs_deniedForNormalUser() {
        when(announceMapper.selectById(5L)).thenReturn(announce(5L, STATUS_PUBLISHED, AUTHOR_ID));
        stubNotManager();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> announceService.sendLogs(5L, USER_ID));

        assertEquals(403, ex.getCode());
        verifyNoInteractions(sendLogMapper);
    }

    /** 普通用户：既非超管，也没有任何通知写权限 */
    private void stubNotManager() {
        when(permissionService.hasRole(USER_ID, "super_admin")).thenReturn(false);
        when(permissionService.hasPermission(USER_ID, "system:announce:create")).thenReturn(false);
        when(permissionService.hasPermission(USER_ID, "system:announce:update")).thenReturn(false);
        when(permissionService.hasPermission(USER_ID, "system:announce:publish")).thenReturn(false);
        when(permissionService.hasPermission(USER_ID, "system:announce:delete")).thenReturn(false);
    }

    private static AnnounceDO announce(long id, int status, long createBy) {
        AnnounceDO announce = new AnnounceDO();
        announce.setId(id);
        announce.setTitle("测试通知");
        announce.setContent("测试正文");
        announce.setStatus(status);
        announce.setCreateBy(createBy);
        return announce;
    }
}

package cn.rbac.server.modules.system.service.message;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.framework.websocket.MessageWebSocketHandler;
import cn.rbac.server.modules.system.dal.dataobject.message.ChatGroupDO;
import cn.rbac.server.modules.system.dal.dataobject.message.ChatGroupMessageDO;
import cn.rbac.server.modules.system.dal.dataobject.message.ChatMessageDO;
import cn.rbac.server.modules.system.dal.dataobject.message.ChatGroupLogDO;
import cn.rbac.server.modules.system.dal.dataobject.message.ChatGroupLatestVO;
import cn.rbac.server.modules.system.dal.dataobject.message.ChatGroupMemberCountVO;
import cn.rbac.server.modules.system.dal.dataobject.message.ChatGroupMemberDO;
import cn.rbac.server.modules.system.dal.dataobject.message.ChatPeerLatestVO;
import cn.rbac.server.modules.system.dal.dataobject.message.ChatPeerUnreadVO;
import cn.rbac.server.modules.system.dal.dataobject.message.UserBlacklistDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.dal.mysql.message.*;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.service.permission.PermissionService;
import cn.rbac.server.testsupport.MybatisLambdaTestBase;
import cn.rbac.server.testsupport.MybatisMockMatchers;
import cn.rbac.server.testsupport.ServiceTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChatService 单元测试")
class ChatServiceTest extends MybatisLambdaTestBase {

    private static final long ME = 1L;

    @Mock
    private ChatMessageMapper chatMessageMapper;
    @Mock
    private UserBlacklistMapper blacklistMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ChatGroupMapper chatGroupMapper;
    @Mock
    private ChatGroupMemberMapper groupMemberMapper;
    @Mock
    private ChatGroupMessageMapper groupMessageMapper;
    @Mock
    private ChatGroupLogMapper groupLogMapper;
    @Mock
    private MessageWebSocketHandler webSocketHandler;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private ChatService chatService;

    @Test
    @DisplayName("listUsers：无其他用户时返回空列表")
    void listUsers_emptyWhenNoPeers() {
        when(userMapper.selectList(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(List.of());

        assertTrue(chatService.listUsers(ME).isEmpty());
        verifyNoInteractions(chatMessageMapper, blacklistMapper);
    }

    @Test
    @DisplayName("listUsers：批量查询黑名单/最新消息/未读数各一次")
    void listUsers_usesBatchQueries() {
        UserDO peer = ServiceTestFixtures.user(2L, "bob", 1);
        when(userMapper.selectList(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(List.of(peer));
        when(blacklistMapper.selectBlockedUserIds(ME)).thenReturn(List.of(2L));

        ChatPeerLatestVO latest = new ChatPeerLatestVO();
        latest.setPeerId(2L);
        latest.setContent("hi");
        latest.setMsgType(1);
        latest.setSendTime(LocalDateTime.now().minusMinutes(1));
        when(chatMessageMapper.selectLatestPerPeer(ME)).thenReturn(List.of(latest));

        ChatPeerUnreadVO unread = new ChatPeerUnreadVO();
        unread.setSenderId(2L);
        unread.setUnreadCount(3L);
        when(chatMessageMapper.selectUnreadCountPerSender(ME)).thenReturn(List.of(unread));
        when(webSocketHandler.isOnline(2L)).thenReturn(true);

        List<Map<String, Object>> result = chatService.listUsers(ME);

        assertEquals(1, result.size());
        Map<String, Object> item = result.get(0);
        assertEquals(2L, item.get("id"));
        assertEquals(true, item.get("isBlocked"));
        assertEquals(3L, item.get("unreadCount"));
        assertNotNull(item.get("lastMessage"));

        verify(blacklistMapper, times(1)).selectBlockedUserIds(ME);
        verify(chatMessageMapper, times(1)).selectLatestPerPeer(ME);
        verify(chatMessageMapper, times(1)).selectUnreadCountPerSender(ME);
    }

    @Test
    @DisplayName("isInMyBlacklist：在黑名单中返回 true")
    void isInMyBlacklist_trueWhenExists() {
        when(blacklistMapper.selectCount(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(1L);

        assertTrue(chatService.isInMyBlacklist(ME, 2L));
    }

    @Test
    @DisplayName("block：未拉黑时插入记录")
    void block_insertsWhenNotBlocked() {
        when(blacklistMapper.selectCount(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(0L);

        chatService.block(ME, 2L);

        verify(blacklistMapper).insert(any(UserBlacklistDO.class));
    }

    @Test
    @DisplayName("myGroups：无群成员关系时返回空列表")
    void myGroups_emptyWhenNoMemberships() {
        when(groupMemberMapper.selectList(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(List.of());

        assertTrue(chatService.myGroups(ME).isEmpty());
        verifyNoInteractions(chatGroupMapper, groupMessageMapper);
    }

    @Test
    @DisplayName("myGroups：批量查询群信息/成员数/最新消息各一次")
    void myGroups_usesBatchQueries() {
        long groupId = 100L;
        ChatGroupMemberDO membership = new ChatGroupMemberDO();
        membership.setGroupId(groupId);
        membership.setUserId(ME);
        membership.setRole(0);
        when(groupMemberMapper.selectList(MybatisMockMatchers.anyLambdaQueryWrapper()))
                .thenReturn(List.of(membership));

        ChatGroupDO group = new ChatGroupDO();
        group.setId(groupId);
        group.setName("研发群");
        group.setOwnerId(2L);
        group.setStatus(1);
        when(chatGroupMapper.selectByIds(List.of(groupId))).thenReturn(List.of(group));

        ChatGroupMemberCountVO memberCount = new ChatGroupMemberCountVO();
        memberCount.setGroupId(groupId);
        memberCount.setMemberCount(5L);
        when(groupMemberMapper.selectMemberCountByGroupIds(List.of(groupId))).thenReturn(List.of(memberCount));

        ChatGroupLatestVO latest = new ChatGroupLatestVO();
        latest.setGroupId(groupId);
        latest.setContent("大家好");
        latest.setMsgType(1);
        latest.setSendTime(LocalDateTime.now().minusHours(1));
        when(groupMessageMapper.selectLatestByGroupIds(List.of(groupId))).thenReturn(List.of(latest));

        List<Map<String, Object>> result = chatService.myGroups(ME);

        assertEquals(1, result.size());
        assertEquals(groupId, result.get(0).get("id"));
        assertEquals(5L, result.get(0).get("memberCount"));
        assertNotNull(result.get(0).get("lastMessage"));

        verify(chatGroupMapper, times(1)).selectByIds(List.of(groupId));
        verify(groupMemberMapper, times(1)).selectMemberCountByGroupIds(List.of(groupId));
        verify(groupMessageMapper, times(1)).selectLatestByGroupIds(List.of(groupId));
    }

    @Test
    @DisplayName("sendPrivate：存在拉黑关系时拒绝发送")
    void sendPrivate_blocked() {
        when(blacklistMapper.selectCount(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(1L, 0L);

        assertThrows(BusinessException.class,
                () -> chatService.sendPrivate(ME, 2L, "hi", 1));
        verify(chatMessageMapper, never()).insert(any(ChatMessageDO.class));
    }

    @Test
    @DisplayName("sendPrivate：成功发送并推送 WebSocket")
    void sendPrivate_success() {
        when(blacklistMapper.selectCount(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(0L);
        when(userMapper.selectById(ME)).thenReturn(ServiceTestFixtures.user(ME, "alice", 1));
        doAnswer(inv -> {
            ChatMessageDO msg = inv.getArgument(0);
            msg.setId(200L);
            return 1;
        }).when(chatMessageMapper).insert(any(ChatMessageDO.class));

        ChatMessageDO msg = chatService.sendPrivate(ME, 2L, "hello", 1);

        assertEquals("hello", msg.getContent());
        assertEquals(2L, msg.getReceiverId());
        verify(webSocketHandler).sendChatPayload(eq(2L), any());
        verify(webSocketHandler).sendTypingStopPayload(2L, ME);
    }

    @Test
    @DisplayName("sendGroupMessage：不在群内时拒绝")
    void sendGroupMessage_notMember() {
        when(groupMemberMapper.selectOne(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> chatService.sendGroupMessage(100L, ME, "hi", 1));
    }

    @Test
    @DisplayName("sendGroupMessage：被禁言时拒绝")
    void sendGroupMessage_muted() {
        ChatGroupMemberDO member = new ChatGroupMemberDO();
        member.setGroupId(100L);
        member.setUserId(ME);
        member.setMuted(1);
        when(groupMemberMapper.selectOne(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(member);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> chatService.sendGroupMessage(100L, ME, "hi", 1));

        assertTrue(ex.getMessage().contains("禁言"));
    }

    @Test
    @DisplayName("recallPrivate：只能撤回自己发送的消息")
    void recallPrivate_onlySender() {
        ChatMessageDO msg = new ChatMessageDO();
        msg.setId(50L);
        msg.setSenderId(99L);
        msg.setSendTime(LocalDateTime.now());
        when(chatMessageMapper.selectById(50L)).thenReturn(msg);

        assertThrows(BusinessException.class, () -> chatService.recallPrivate(ME, 50L));
    }

    @Test
    @DisplayName("recallPrivate：成功撤回消息")
    void recallPrivate_success() {
        ChatMessageDO msg = new ChatMessageDO();
        msg.setId(50L);
        msg.setSenderId(ME);
        msg.setReceiverId(2L);
        msg.setSendTime(LocalDateTime.now());
        msg.setMsgType(ChatMsgType.TEXT);
        when(chatMessageMapper.selectById(50L)).thenReturn(msg);

        ChatMessageDO recalled = chatService.recallPrivate(ME, 50L);

        assertEquals(ChatMsgType.RECALLED, recalled.getMsgType());
        assertEquals("", recalled.getContent());
        verify(chatMessageMapper).updateById(msg);
    }

    @Test
    @DisplayName("recallGroupMessage：不在群内时拒绝")
    void recallGroupMessage_notMember() {
        when(groupMemberMapper.selectOne(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> chatService.recallGroupMessage(100L, ME, 50L));
    }

    @Test
    @DisplayName("canCreateGroup：仅 super_admin 可建群")
    void canCreateGroup_superAdminOnly() {
        when(permissionService.hasRole(ME, "super_admin")).thenReturn(true);
        when(permissionService.hasRole(2L, "super_admin")).thenReturn(false);

        assertTrue(chatService.canCreateGroup(ME));
        assertFalse(chatService.canCreateGroup(2L));
        assertFalse(chatService.canCreateGroup(null));
    }

    @Test
    @DisplayName("createGroup：超级管理员可建群")
    void createGroup_success() {
        when(permissionService.hasRole(ME, "super_admin")).thenReturn(true);
        doAnswer(inv -> {
            ChatGroupDO g = inv.getArgument(0);
            g.setId(100L);
            return 1;
        }).when(chatGroupMapper).insert(any(ChatGroupDO.class));
        when(userMapper.selectById(ME)).thenReturn(ServiceTestFixtures.user(ME, "alice", 1));
        when(userMapper.selectById(2L)).thenReturn(ServiceTestFixtures.user(2L, "bob", 1));

        ChatGroupDO group = chatService.createGroup(ME, "研发群", List.of(2L));

        assertEquals("研发群", group.getName());
        assertEquals(ME, group.getOwnerId());
        verify(groupMemberMapper).insertBatch(argThat(list -> list != null && list.size() == 2));
        verify(groupLogMapper, atLeast(1)).insert(any(ChatGroupLogDO.class));
    }

    @Test
    @DisplayName("createGroup：非超级管理员拒绝")
    void createGroup_forbidden() {
        when(permissionService.hasRole(ME, "super_admin")).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> chatService.createGroup(ME, "研发群", List.of()));
    }

    @Test
    @DisplayName("addMembers：管理员可邀请新成员")
    void addMembers_success() {
        ChatGroupMemberDO operator = new ChatGroupMemberDO();
        operator.setGroupId(100L);
        operator.setUserId(ME);
        operator.setRole(2);
        when(groupMemberMapper.selectOne(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(operator);
        when(groupMemberMapper.selectCount(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(0L);
        when(userMapper.selectById(2L)).thenReturn(ServiceTestFixtures.user(2L, "bob", 1));

        chatService.addMembers(100L, List.of(2L), ME);

        verify(groupMemberMapper).insertBatch(argThat(list -> list != null && list.size() == 1));
        verify(groupLogMapper).insert(any(ChatGroupLogDO.class));
    }

    @Test
    @DisplayName("dissolveGroup：仅群主可解散")
    void dissolveGroup_success() {
        ChatGroupDO group = new ChatGroupDO();
        group.setId(100L);
        group.setOwnerId(ME);
        group.setStatus(1);
        when(chatGroupMapper.selectById(100L)).thenReturn(group);

        chatService.dissolveGroup(100L, ME);

        verify(chatGroupMapper).updateById(argThat((ChatGroupDO g) -> g.getStatus() == 0));
    }

    @Test
    @DisplayName("dissolveGroup：非群主拒绝")
    void dissolveGroup_forbidden() {
        ChatGroupDO group = new ChatGroupDO();
        group.setId(100L);
        group.setOwnerId(99L);
        when(chatGroupMapper.selectById(100L)).thenReturn(group);

        assertThrows(BusinessException.class, () -> chatService.dissolveGroup(100L, ME));
    }

    @Test
    @DisplayName("sendGroupMessage：成功发送并推送给其他成员")
    void sendGroupMessage_success() {
        ChatGroupMemberDO sender = new ChatGroupMemberDO();
        sender.setGroupId(100L);
        sender.setUserId(ME);
        sender.setMuted(0);
        ChatGroupMemberDO peer = new ChatGroupMemberDO();
        peer.setGroupId(100L);
        peer.setUserId(2L);
        when(groupMemberMapper.selectOne(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(sender);
        when(groupMemberMapper.selectList(MybatisMockMatchers.anyLambdaQueryWrapper()))
                .thenReturn(List.of(sender, peer));
        when(userMapper.selectById(ME)).thenReturn(ServiceTestFixtures.user(ME, "alice", 1));
        doAnswer(inv -> {
            ChatGroupMessageDO msg = inv.getArgument(0);
            msg.setId(300L);
            return 1;
        }).when(groupMessageMapper).insert(any(ChatGroupMessageDO.class));

        ChatGroupMessageDO msg = chatService.sendGroupMessage(100L, ME, "大家好", 1);

        assertEquals("大家好", msg.getContent());
        verify(webSocketHandler).sendGroupChatPayload(eq(2L), any());
    }

    @Test
    @DisplayName("recallGroupMessage：成功撤回群消息")
    void recallGroupMessage_success() {
        ChatGroupMemberDO member = new ChatGroupMemberDO();
        member.setGroupId(100L);
        member.setUserId(ME);
        when(groupMemberMapper.selectOne(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(member);

        ChatGroupMessageDO msg = new ChatGroupMessageDO();
        msg.setId(50L);
        msg.setGroupId(100L);
        msg.setSenderId(ME);
        msg.setSendTime(LocalDateTime.now());
        msg.setMsgType(ChatMsgType.TEXT);
        when(groupMessageMapper.selectById(50L)).thenReturn(msg);

        ChatGroupMessageDO recalled = chatService.recallGroupMessage(100L, ME, 50L);

        assertEquals(ChatMsgType.RECALLED, recalled.getMsgType());
        verify(groupMessageMapper).updateById(msg);
    }
}

package cn.rbac.server.modules.system.service.message;

import cn.rbac.server.framework.websocket.MessageWebSocketHandler;
import cn.rbac.server.modules.system.service.permission.PermissionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.rbac.server.modules.system.dal.dataobject.message.*;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.dal.mysql.message.*;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.service.message.vo.ChatGroupLogVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final int RECALL_MINUTES = 2;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private ChatMessageMapper chatMessageMapper;
    @Resource
    private UserBlacklistMapper blacklistMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private ChatGroupMapper chatGroupMapper;
    @Resource
    private ChatGroupMemberMapper groupMemberMapper;
    @Resource
    private ChatGroupMessageMapper groupMessageMapper;
    @Resource
    private ChatGroupLogMapper groupLogMapper;
    @Resource
    private MessageWebSocketHandler webSocketHandler;
    @Resource
    private PermissionService permissionService;

    public boolean canCreateGroup(Long userId) {
        return userId != null && permissionService.hasRole(userId, "super_admin");
    }

    public boolean isBlocked(Long senderId, Long receiverId) {
        Long c1 = blacklistMapper.selectCount(new LambdaQueryWrapper<UserBlacklistDO>()
                .eq(UserBlacklistDO::getUserId, receiverId)
                .eq(UserBlacklistDO::getBlockedUserId, senderId));
        Long c2 = blacklistMapper.selectCount(new LambdaQueryWrapper<UserBlacklistDO>()
                .eq(UserBlacklistDO::getUserId, senderId)
                .eq(UserBlacklistDO::getBlockedUserId, receiverId));
        return c1 > 0 || c2 > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public ChatMessageDO sendPrivate(Long senderId, Long receiverId, String content, Integer msgType) {
        if (isBlocked(senderId, receiverId)) {
            throw new IllegalStateException("无法发送，存在拉黑关系");
        }
        UserDO sender = userMapper.selectById(senderId);
        ChatMessageDO msg = new ChatMessageDO();
        msg.setSenderId(senderId);
        msg.setSenderName(sender != null ? sender.getNickname() : "");
        msg.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        msg.setMsgType(msgType == null ? 1 : msgType);
        msg.setIsRead(0);
        msg.setSendTime(LocalDateTime.now());
        chatMessageMapper.insert(msg);
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", msg.getId());
        payload.put("senderId", msg.getSenderId());
        payload.put("senderName", msg.getSenderName());
        payload.put("senderAvatar", msg.getSenderAvatar());
        payload.put("content", msg.getContent());
        payload.put("msgType", msg.getMsgType());
        webSocketHandler.sendChatPayload(receiverId, payload);
        webSocketHandler.sendTypingStopPayload(receiverId, senderId);
        return msg;
    }

    public Page<ChatMessageDO> history(Long userId, Long targetId, int pageNo, int pageSize) {
        LambdaQueryWrapper<ChatMessageDO> w = new LambdaQueryWrapper<ChatMessageDO>()
                .and(q -> q.and(a -> a.eq(ChatMessageDO::getSenderId, userId).eq(ChatMessageDO::getReceiverId, targetId))
                        .or(b -> b.eq(ChatMessageDO::getSenderId, targetId).eq(ChatMessageDO::getReceiverId, userId)))
                .orderByDesc(ChatMessageDO::getSendTime);
        Page<ChatMessageDO> page = chatMessageMapper.selectPage(new Page<>(pageNo, pageSize), w);
        Collections.reverse(page.getRecords());
        return page;
    }

    public List<Map<String, Object>> listUsers(Long userId) {
        List<UserDO> users = userMapper.selectList(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getStatus, 1)
                .ne(UserDO::getId, userId));
        if (users.isEmpty()) {
            return List.of();
        }

        Set<Long> blockedIds = new HashSet<>(blacklistMapper.selectBlockedUserIds(userId));
        Map<Long, ChatPeerLatestVO> latestByPeer = chatMessageMapper.selectLatestPerPeer(userId).stream()
                .collect(Collectors.toMap(ChatPeerLatestVO::getPeerId, v -> v, (a, b) -> a));
        Map<Long, Long> unreadBySender = chatMessageMapper.selectUnreadCountPerSender(userId).stream()
                .collect(Collectors.toMap(ChatPeerUnreadVO::getSenderId, ChatPeerUnreadVO::getUnreadCount));

        List<Map<String, Object>> result = new ArrayList<>(users.size());
        for (UserDO u : users) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", u.getId());
            item.put("username", u.getUsername());
            item.put("nickname", u.getNickname());
            item.put("avatar", u.getAvatar());
            item.put("isBlocked", blockedIds.contains(u.getId()));
            item.put("online", webSocketHandler.isOnline(u.getId()));
            ChatPeerLatestVO latest = latestByPeer.get(u.getId());
            if (latest != null) {
                item.put("lastMessage", ChatMsgType.previewLabel(latest.getMsgType(), latest.getContent()));
                item.put("lastMessageTime", latest.getSendTime());
            }
            item.put("unreadCount", unreadBySender.getOrDefault(u.getId(), 0L));
            result.add(item);
        }
        result.sort((a, b) -> {
            LocalDateTime ta = (LocalDateTime) a.get("lastMessageTime");
            LocalDateTime tb = (LocalDateTime) b.get("lastMessageTime");
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long senderId) {
        chatMessageMapper.update(null, new LambdaUpdateWrapper<ChatMessageDO>()
                .eq(ChatMessageDO::getReceiverId, userId)
                .eq(ChatMessageDO::getSenderId, senderId)
                .eq(ChatMessageDO::getIsRead, 0)
                .set(ChatMessageDO::getIsRead, 1));
    }

    public long unreadCount(Long userId) {
        return chatMessageMapper.countUnread(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void clearHistory(Long userId, Long targetId) {
        chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessageDO>()
                .and(q -> q.and(a -> a.eq(ChatMessageDO::getSenderId, userId).eq(ChatMessageDO::getReceiverId, targetId))
                        .or(b -> b.eq(ChatMessageDO::getSenderId, targetId).eq(ChatMessageDO::getReceiverId, userId))));
    }

    public void block(Long userId, Long targetId) {
        if (blacklistMapper.selectCount(new LambdaQueryWrapper<UserBlacklistDO>()
                .eq(UserBlacklistDO::getUserId, userId)
                .eq(UserBlacklistDO::getBlockedUserId, targetId)) == 0) {
            UserBlacklistDO b = new UserBlacklistDO();
            b.setUserId(userId);
            b.setBlockedUserId(targetId);
            b.setCreateTime(LocalDateTime.now());
            blacklistMapper.insert(b);
        }
    }

    public void unblock(Long userId, Long targetId) {
        blacklistMapper.delete(new LambdaQueryWrapper<UserBlacklistDO>()
                .eq(UserBlacklistDO::getUserId, userId)
                .eq(UserBlacklistDO::getBlockedUserId, targetId));
    }

    public boolean isInMyBlacklist(Long userId, Long targetId) {
        return blacklistMapper.selectCount(new LambdaQueryWrapper<UserBlacklistDO>()
                .eq(UserBlacklistDO::getUserId, userId)
                .eq(UserBlacklistDO::getBlockedUserId, targetId)) > 0;
    }

  // ---- group ----

    @Transactional(rollbackFor = Exception.class)
    public ChatGroupDO createGroup(Long ownerId, String name, List<Long> memberIds) {
        if (!canCreateGroup(ownerId)) {
            throw new IllegalStateException("仅超级管理员可创建群聊");
        }
        ChatGroupDO group = new ChatGroupDO();
        group.setName(name);
        group.setOwnerId(ownerId);
        group.setStatus(1);
        group.setCreateTime(LocalDateTime.now());
        group.setUpdateTime(LocalDateTime.now());
        chatGroupMapper.insert(group);
        Set<Long> ids = new HashSet<>(memberIds == null ? List.of() : memberIds);
        ids.add(ownerId);
        List<String> invitedNames = new ArrayList<>();
        for (Long uid : ids) {
            ChatGroupMemberDO m = new ChatGroupMemberDO();
            m.setGroupId(group.getId());
            m.setUserId(uid);
            m.setRole(uid.equals(ownerId) ? 2 : 0);
            m.setJoinTime(LocalDateTime.now());
            groupMemberMapper.insert(m);
            if (!uid.equals(ownerId)) {
                invitedNames.add(displayUserName(userMapper.selectById(uid)));
            }
        }
        saveGroupLog(group.getId(), "CREATE", ownerId, null, null, "群名称：" + name);
        if (!invitedNames.isEmpty()) {
            saveGroupLog(group.getId(), "INVITE", ownerId, null, String.join("、", invitedNames), null);
        }
        return group;
    }

    public List<Map<String, Object>> myGroups(Long userId) {
        List<ChatGroupMemberDO> memberships = groupMemberMapper.selectList(
                new LambdaQueryWrapper<ChatGroupMemberDO>().eq(ChatGroupMemberDO::getUserId, userId));
        if (memberships.isEmpty()) {
            return List.of();
        }

        List<Long> groupIds = memberships.stream().map(ChatGroupMemberDO::getGroupId).distinct().toList();
        Map<Long, ChatGroupDO> groupMap = chatGroupMapper.selectByIds(groupIds).stream()
                .filter(g -> g.getStatus() != null && g.getStatus() == 1)
                .collect(Collectors.toMap(ChatGroupDO::getId, g -> g, (a, b) -> a));
        if (groupMap.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> memberCountMap = groupMemberMapper.selectMemberCountByGroupIds(groupIds).stream()
                .collect(Collectors.toMap(ChatGroupMemberCountVO::getGroupId, ChatGroupMemberCountVO::getMemberCount));
        Map<Long, ChatGroupLatestVO> latestMsgMap = groupMessageMapper.selectLatestByGroupIds(groupIds).stream()
                .collect(Collectors.toMap(ChatGroupLatestVO::getGroupId, v -> v, (a, b) -> a));

        List<Map<String, Object>> list = new ArrayList<>();
        for (ChatGroupMemberDO m : memberships) {
            ChatGroupDO g = groupMap.get(m.getGroupId());
            if (g == null) {
                continue;
            }
            Map<String, Object> item = buildGroupSummary(g, m);
            item.put("memberCount", memberCountMap.getOrDefault(g.getId(), 0L));
            ChatGroupLatestVO last = latestMsgMap.get(g.getId());
            if (last != null) {
                item.put("lastMessage", ChatMsgType.previewLabel(last.getMsgType(), last.getContent()));
                item.put("lastMessageTime", last.getSendTime());
            }
            enrichAnnouncementPublisher(item, g.getId());
            list.add(item);
        }
        return list;
    }

    @Transactional(rollbackFor = Exception.class)
    public ChatGroupMessageDO sendGroupMessage(Long groupId, Long senderId, String content, Integer msgType) {
        return sendGroupMessage(groupId, senderId, content, msgType, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public ChatGroupMessageDO sendGroupMessage(Long groupId, Long senderId, String content, Integer msgType,
            List<Long> mentionIds) {
        ChatGroupMemberDO member = groupMemberMapper.selectOne(new LambdaQueryWrapper<ChatGroupMemberDO>()
                .eq(ChatGroupMemberDO::getGroupId, groupId)
                .eq(ChatGroupMemberDO::getUserId, senderId));
        if (member == null) {
            throw new IllegalStateException("不在该群");
        }
        if (member.getMuted() != null && member.getMuted() == 1) {
            throw new IllegalStateException("您已被禁言");
        }
        UserDO sender = userMapper.selectById(senderId);
        ChatGroupMessageDO msg = new ChatGroupMessageDO();
        msg.setGroupId(groupId);
        msg.setSenderId(senderId);
        msg.setSenderName(sender != null ? sender.getNickname() : "");
        msg.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        msg.setContent(content);
        msg.setMsgType(msgType == null ? ChatMsgType.TEXT : msgType);
        msg.setSendTime(LocalDateTime.now());
        List<Long> atIds = normalizeMentionIds(mentionIds);
        if (!atIds.isEmpty()) {
            try {
                msg.setMentionIds(objectMapper.writeValueAsString(atIds));
            } catch (Exception e) {
                throw new IllegalStateException("提及用户解析失败");
            }
        }
        groupMessageMapper.insert(msg);
        List<ChatGroupMemberDO> members = groupMemberMapper.selectList(
                new LambdaQueryWrapper<ChatGroupMemberDO>().eq(ChatGroupMemberDO::getGroupId, groupId));
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", msg.getId());
        payload.put("groupId", groupId);
        payload.put("senderId", senderId);
        payload.put("senderName", msg.getSenderName());
        payload.put("senderAvatar", msg.getSenderAvatar());
        payload.put("content", content);
        payload.put("msgType", msg.getMsgType());
        payload.put("mentionIds", atIds);
        for (ChatGroupMemberDO m : members) {
            if (!m.getUserId().equals(senderId)) {
                Map<String, Object> memberPayload = new LinkedHashMap<>(payload);
                memberPayload.put("atMe", atIds.contains(m.getUserId()));
                webSocketHandler.sendGroupChatPayload(m.getUserId(), memberPayload);
            }
        }
        return msg;
    }

    @Transactional(rollbackFor = Exception.class)
    public ChatMessageDO recallPrivate(Long userId, Long messageId) {
        ChatMessageDO msg = chatMessageMapper.selectById(messageId);
        if (msg == null) {
            throw new IllegalStateException("消息不存在");
        }
        if (!userId.equals(msg.getSenderId())) {
            throw new IllegalStateException("只能撤回自己发送的消息");
        }
        ensureWithinRecallWindow(msg.getSendTime());
        if (msg.getMsgType() != null && msg.getMsgType() == ChatMsgType.RECALLED) {
            return msg;
        }
        msg.setMsgType(ChatMsgType.RECALLED);
        msg.setContent("");
        chatMessageMapper.updateById(msg);
        broadcastPrivateRecall(msg);
        return msg;
    }

    @Transactional(rollbackFor = Exception.class)
    public ChatGroupMessageDO recallGroupMessage(Long groupId, Long userId, Long messageId) {
        ChatGroupMemberDO member = groupMemberMapper.selectOne(new LambdaQueryWrapper<ChatGroupMemberDO>()
                .eq(ChatGroupMemberDO::getGroupId, groupId)
                .eq(ChatGroupMemberDO::getUserId, userId));
        if (member == null) {
            throw new IllegalStateException("不在该群");
        }
        ChatGroupMessageDO msg = groupMessageMapper.selectById(messageId);
        if (msg == null || !groupId.equals(msg.getGroupId())) {
            throw new IllegalStateException("消息不存在");
        }
        if (!userId.equals(msg.getSenderId())) {
            throw new IllegalStateException("只能撤回自己发送的消息");
        }
        ensureWithinRecallWindow(msg.getSendTime());
        if (msg.getMsgType() != null && msg.getMsgType() == ChatMsgType.RECALLED) {
            return msg;
        }
        msg.setMsgType(ChatMsgType.RECALLED);
        msg.setContent("");
        groupMessageMapper.updateById(msg);
        broadcastGroupRecall(msg);
        return msg;
    }

    public void relayTyping(Long fromUserId, Long toUserId) {
        if (fromUserId == null || toUserId == null || fromUserId.equals(toUserId)) {
            return;
        }
        webSocketHandler.sendTypingPayload(toUserId, fromUserId);
    }

    private void ensureWithinRecallWindow(LocalDateTime sendTime) {
        if (sendTime == null) {
            return;
        }
        if (Duration.between(sendTime, LocalDateTime.now()).toMinutes() > RECALL_MINUTES) {
            throw new IllegalStateException("已超过 " + RECALL_MINUTES + " 分钟，无法撤回");
        }
    }

    private void broadcastPrivateRecall(ChatMessageDO msg) {
        Map<String, Object> payload = recallPayload(msg.getId(), msg.getSenderId(), null, msg.getSenderName());
        webSocketHandler.sendChatPayload(msg.getReceiverId(), payload);
        webSocketHandler.sendChatPayload(msg.getSenderId(), payload);
    }

    private void broadcastGroupRecall(ChatGroupMessageDO msg) {
        List<ChatGroupMemberDO> members = groupMemberMapper.selectList(
                new LambdaQueryWrapper<ChatGroupMemberDO>().eq(ChatGroupMemberDO::getGroupId, msg.getGroupId()));
        Map<String, Object> payload = recallPayload(msg.getId(), msg.getSenderId(), msg.getGroupId(), msg.getSenderName());
        for (ChatGroupMemberDO m : members) {
            webSocketHandler.sendGroupChatPayload(m.getUserId(), payload);
        }
    }

    private Map<String, Object> recallPayload(Long messageId, Long senderId, Long groupId, String senderName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("recall", true);
        payload.put("id", messageId);
        payload.put("messageId", messageId);
        payload.put("senderId", senderId);
        payload.put("senderName", senderName);
        payload.put("msgType", ChatMsgType.RECALLED);
        if (groupId != null) {
            payload.put("groupId", groupId);
        }
        return payload;
    }

    private List<Long> normalizeMentionIds(List<Long> mentionIds) {
        if (mentionIds == null || mentionIds.isEmpty()) {
            return List.of();
        }
        return mentionIds.stream().filter(Objects::nonNull).distinct().toList();
    }

    public List<Long> parseMentionIds(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    public Page<ChatGroupMessageDO> groupMessages(Long groupId, Long userId, int pageNo, int pageSize) {
        ChatGroupMemberDO member = groupMemberMapper.selectOne(new LambdaQueryWrapper<ChatGroupMemberDO>()
                .eq(ChatGroupMemberDO::getGroupId, groupId)
                .eq(ChatGroupMemberDO::getUserId, userId));
        if (member == null) {
            throw new IllegalStateException("不在该群");
        }
        Page<ChatGroupMessageDO> page = groupMessageMapper.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<ChatGroupMessageDO>()
                        .eq(ChatGroupMessageDO::getGroupId, groupId)
                        .orderByDesc(ChatGroupMessageDO::getSendTime));
        Collections.reverse(page.getRecords());
        return page;
    }

    public List<Map<String, Object>> groupMembers(Long groupId) {
        List<ChatGroupMemberDO> members = groupMemberMapper.selectList(new LambdaQueryWrapper<ChatGroupMemberDO>()
                .eq(ChatGroupMemberDO::getGroupId, groupId));
        List<Map<String, Object>> list = new ArrayList<>();
        for (ChatGroupMemberDO m : members) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", m.getId());
            item.put("groupId", m.getGroupId());
            item.put("userId", m.getUserId());
            item.put("nickname", m.getNickname());
            item.put("role", m.getRole());
            item.put("muted", m.getMuted());
            item.put("joinTime", m.getJoinTime());
            UserDO user = userMapper.selectById(m.getUserId());
            if (user != null) {
                item.put("userNickname", user.getNickname());
                item.put("username", user.getUsername());
                item.put("avatar", user.getAvatar());
            }
            list.add(item);
        }
        return list;
    }

    public ChatGroupDO getGroupDetail(Long groupId) {
        return chatGroupMapper.selectById(groupId);
    }

    public Map<String, Object> getGroupContext(Long groupId, Long userId) {
        ChatGroupDO group = chatGroupMapper.selectById(groupId);
        if (group == null || group.getStatus() == null || group.getStatus() != 1) {
            throw new IllegalStateException("群不存在或已解散");
        }
        ChatGroupMemberDO member = groupMemberMapper.selectOne(new LambdaQueryWrapper<ChatGroupMemberDO>()
                .eq(ChatGroupMemberDO::getGroupId, groupId)
                .eq(ChatGroupMemberDO::getUserId, userId));
        if (member == null) {
            throw new IllegalStateException("不在该群");
        }
        Map<String, Object> item = buildGroupSummary(group, member);
        long count = groupMemberMapper.selectCount(new LambdaQueryWrapper<ChatGroupMemberDO>()
                .eq(ChatGroupMemberDO::getGroupId, groupId));
        item.put("memberCount", count);
        enrichAnnouncementPublisher(item, groupId);
        return item;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateGroup(Long groupId, Long userId, String name, String announcement) {
        requireGroupAdmin(groupId, userId);
        ChatGroupDO group = chatGroupMapper.selectById(groupId);
        if (group == null || group.getStatus() == null || group.getStatus() != 1) {
            throw new IllegalStateException("群不存在或已解散");
        }
        StringBuilder detail = new StringBuilder();
        boolean announcementChanged = false;
        if (StringUtils.hasText(name) && !name.equals(group.getName())) {
            detail.append("群名称改为「").append(name).append("」");
            group.setName(name);
        }
        if (announcement != null && !Objects.equals(announcement, group.getAnnouncement())) {
            if (detail.length() > 0) {
                detail.append("；");
            }
            detail.append("更新了群公告");
            group.setAnnouncement(announcement);
            announcementChanged = true;
        }
        if (detail.isEmpty()) {
            return;
        }
        group.setUpdateTime(LocalDateTime.now());
        chatGroupMapper.updateById(group);
        if (announcementChanged) {
            groupMemberMapper.update(null, new LambdaUpdateWrapper<ChatGroupMemberDO>()
                    .eq(ChatGroupMemberDO::getGroupId, groupId)
                    .set(ChatGroupMemberDO::getAnnouncementReadTime, null));
        }
        saveGroupLog(groupId, "UPDATE", userId, null, null, detail.toString());
        if (announcementChanged) {
            pushGroupAnnouncement(groupId, userId, group.getName(), group.getAnnouncement());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void markAnnouncementRead(Long groupId, Long userId) {
        ChatGroupMemberDO member = groupMemberMapper.selectOne(new LambdaQueryWrapper<ChatGroupMemberDO>()
                .eq(ChatGroupMemberDO::getGroupId, groupId)
                .eq(ChatGroupMemberDO::getUserId, userId));
        if (member == null) {
            throw new IllegalStateException("不在该群");
        }
        member.setAnnouncementReadTime(LocalDateTime.now());
        groupMemberMapper.updateById(member);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setNotifyMuted(Long groupId, Long userId, boolean notifyMuted) {
        ChatGroupMemberDO member = groupMemberMapper.selectOne(new LambdaQueryWrapper<ChatGroupMemberDO>()
                .eq(ChatGroupMemberDO::getGroupId, groupId)
                .eq(ChatGroupMemberDO::getUserId, userId));
        if (member == null) {
            throw new IllegalStateException("不在该群");
        }
        member.setNotifyMuted(notifyMuted ? 1 : 0);
        groupMemberMapper.updateById(member);
    }

    @Transactional(rollbackFor = Exception.class)
    public void addMembers(Long groupId, List<Long> userIds, Long operatorId) {
        requireGroupAdmin(groupId, operatorId);
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<String> invitedNames = new ArrayList<>();
        for (Long uid : userIds) {
            Long exists = groupMemberMapper.selectCount(new LambdaQueryWrapper<ChatGroupMemberDO>()
                    .eq(ChatGroupMemberDO::getGroupId, groupId)
                    .eq(ChatGroupMemberDO::getUserId, uid));
            if (exists > 0) {
                continue;
            }
            ChatGroupMemberDO m = new ChatGroupMemberDO();
            m.setGroupId(groupId);
            m.setUserId(uid);
            m.setRole(0);
            m.setJoinTime(LocalDateTime.now());
            groupMemberMapper.insert(m);
            invitedNames.add(displayUserName(userMapper.selectById(uid)));
        }
        if (!invitedNames.isEmpty()) {
            saveGroupLog(groupId, "INVITE", operatorId, null, String.join("、", invitedNames), null);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long groupId, Long memberUserId, Long operatorId) {
        ChatGroupMemberDO operator = requireGroupAdmin(groupId, operatorId);
        ChatGroupMemberDO target = groupMemberMapper.selectOne(new LambdaQueryWrapper<ChatGroupMemberDO>()
                .eq(ChatGroupMemberDO::getGroupId, groupId)
                .eq(ChatGroupMemberDO::getUserId, memberUserId));
        if (target == null) {
            throw new IllegalStateException("成员不存在");
        }
        if (target.getRole() != null && target.getRole() >= 2) {
            throw new IllegalStateException("不能移除群主");
        }
        if (operator.getRole() != null && operator.getRole() == 1
                && target.getRole() != null && target.getRole() >= 1) {
            throw new IllegalStateException("管理员不能移除同级或上级");
        }
        saveGroupLog(groupId, "REMOVE", operatorId, memberUserId, null, null);
        groupMemberMapper.deleteById(target.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void setAdmin(Long groupId, Long memberUserId, Long operatorId, boolean admin) {
        ChatGroupDO group = chatGroupMapper.selectById(groupId);
        if (group == null || !operatorId.equals(group.getOwnerId())) {
            throw new IllegalStateException("仅群主可设置管理员");
        }
        ChatGroupMemberDO target = groupMemberMapper.selectOne(new LambdaQueryWrapper<ChatGroupMemberDO>()
                .eq(ChatGroupMemberDO::getGroupId, groupId)
                .eq(ChatGroupMemberDO::getUserId, memberUserId));
        if (target == null) {
            throw new IllegalStateException("成员不存在");
        }
        if (target.getRole() != null && target.getRole() >= 2) {
            throw new IllegalStateException("不能修改群主角色");
        }
        target.setRole(admin ? 1 : 0);
        groupMemberMapper.updateById(target);
        saveGroupLog(groupId, admin ? "SET_ADMIN" : "REMOVE_ADMIN", operatorId, memberUserId, null, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setMuted(Long groupId, Long memberUserId, Long operatorId, boolean muted) {
        ChatGroupMemberDO operator = requireGroupAdmin(groupId, operatorId);
        ChatGroupMemberDO target = groupMemberMapper.selectOne(new LambdaQueryWrapper<ChatGroupMemberDO>()
                .eq(ChatGroupMemberDO::getGroupId, groupId)
                .eq(ChatGroupMemberDO::getUserId, memberUserId));
        if (target == null) {
            throw new IllegalStateException("成员不存在");
        }
        if (operator.getRole() != null && operator.getRole() == 1
                && target.getRole() != null && target.getRole() >= 1) {
            throw new IllegalStateException("无权操作该成员");
        }
        target.setMuted(muted ? 1 : 0);
        groupMemberMapper.updateById(target);
        saveGroupLog(groupId, muted ? "MUTE" : "UNMUTE", operatorId, memberUserId, null, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void transferOwner(Long groupId, Long newOwnerId, Long operatorId) {
        ChatGroupDO group = chatGroupMapper.selectById(groupId);
        if (group == null || !operatorId.equals(group.getOwnerId())) {
            throw new IllegalStateException("仅群主可转让");
        }
        ChatGroupMemberDO newOwner = groupMemberMapper.selectOne(new LambdaQueryWrapper<ChatGroupMemberDO>()
                .eq(ChatGroupMemberDO::getGroupId, groupId)
                .eq(ChatGroupMemberDO::getUserId, newOwnerId));
        if (newOwner == null) {
            throw new IllegalStateException("新群主必须是群成员");
        }
        ChatGroupMemberDO oldOwner = groupMemberMapper.selectOne(new LambdaQueryWrapper<ChatGroupMemberDO>()
                .eq(ChatGroupMemberDO::getGroupId, groupId)
                .eq(ChatGroupMemberDO::getUserId, operatorId));
        if (oldOwner != null) {
            oldOwner.setRole(0);
            groupMemberMapper.updateById(oldOwner);
        }
        newOwner.setRole(2);
        groupMemberMapper.updateById(newOwner);
        group.setOwnerId(newOwnerId);
        group.setUpdateTime(LocalDateTime.now());
        chatGroupMapper.updateById(group);
        saveGroupLog(groupId, "TRANSFER_OWNER", operatorId, newOwnerId, null, null);
    }

    private Map<String, Object> buildGroupSummary(ChatGroupDO g, ChatGroupMemberDO m) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", g.getId());
        item.put("name", g.getName());
        item.put("ownerId", g.getOwnerId());
        item.put("announcement", g.getAnnouncement());
        item.put("updateTime", g.getUpdateTime());
        if (m != null) {
            item.put("myRole", m.getRole());
            item.put("notifyMuted", m.getNotifyMuted() != null && m.getNotifyMuted() == 1);
            item.put("announcementUnread", isAnnouncementUnread(g, m));
        }
        return item;
    }

    private void enrichAnnouncementPublisher(Map<String, Object> item, Long groupId) {
        Object announcement = item.get("announcement");
        if (!(announcement instanceof String text) || !StringUtils.hasText(text)) {
            return;
        }
        ChatGroupLogDO log = groupLogMapper.selectOne(new LambdaQueryWrapper<ChatGroupLogDO>()
                .eq(ChatGroupLogDO::getGroupId, groupId)
                .eq(ChatGroupLogDO::getActionType, "UPDATE")
                .like(ChatGroupLogDO::getDetail, "群公告")
                .orderByDesc(ChatGroupLogDO::getCreateTime)
                .last("LIMIT 1"));
        Long publisherId = log != null ? log.getOperatorId() : null;
        LocalDateTime publishTime = log != null ? log.getCreateTime() : null;
        if (publisherId == null) {
            ChatGroupLogDO lastUpdate = groupLogMapper.selectOne(new LambdaQueryWrapper<ChatGroupLogDO>()
                    .eq(ChatGroupLogDO::getGroupId, groupId)
                    .eq(ChatGroupLogDO::getActionType, "UPDATE")
                    .orderByDesc(ChatGroupLogDO::getCreateTime)
                    .last("LIMIT 1"));
            if (lastUpdate != null) {
                publisherId = lastUpdate.getOperatorId();
                publishTime = lastUpdate.getCreateTime();
            }
        }
        if (publisherId == null) {
            Object ownerId = item.get("ownerId");
            if (ownerId instanceof Number n) {
                publisherId = n.longValue();
            }
            Object updateTime = item.get("updateTime");
            if (updateTime instanceof LocalDateTime ut) {
                publishTime = ut;
            }
        }
        if (publisherId != null) {
            UserDO publisher = userMapper.selectById(publisherId);
            item.put("announcementPublisherId", publisherId);
            item.put("announcementPublisherName", displayUserName(publisher));
            if (publisher != null) {
                item.put("announcementPublisherAvatar", publisher.getAvatar());
            }
            item.put("announcementPublishTime", publishTime);
        }
    }

    private void pushGroupAnnouncement(Long groupId, Long publisherId, String groupName, String announcement) {
        UserDO publisher = userMapper.selectById(publisherId);
        String publisherName = displayUserName(publisher);
        String preview = announcement != null ? announcement : "";
        if (preview.length() > 80) {
            preview = preview.substring(0, 80) + "…";
        }
        List<ChatGroupMemberDO> members = groupMemberMapper.selectList(
                new LambdaQueryWrapper<ChatGroupMemberDO>().eq(ChatGroupMemberDO::getGroupId, groupId));
        for (ChatGroupMemberDO m : members) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "groupAnnouncement");
            payload.put("groupId", groupId);
            payload.put("groupName", groupName);
            payload.put("senderId", publisherId);
            payload.put("senderName", publisherName);
            payload.put("title", (groupName != null ? groupName : "群聊") + " 发布了新公告");
            payload.put("content", preview);
            payload.put("announcement", announcement);
            webSocketHandler.sendGroupChatPayload(m.getUserId(), payload);
        }
    }

    private boolean isAnnouncementUnread(ChatGroupDO group, ChatGroupMemberDO member) {
        if (!StringUtils.hasText(group.getAnnouncement())) {
            return false;
        }
        if (member.getAnnouncementReadTime() == null) {
            return true;
        }
        LocalDateTime updated = group.getUpdateTime();
        if (updated == null) {
            return false;
        }
        return member.getAnnouncementReadTime().isBefore(updated);
    }

    private ChatGroupMemberDO requireGroupAdmin(Long groupId, Long userId) {
        ChatGroupMemberDO member = groupMemberMapper.selectOne(new LambdaQueryWrapper<ChatGroupMemberDO>()
                .eq(ChatGroupMemberDO::getGroupId, groupId)
                .eq(ChatGroupMemberDO::getUserId, userId));
        if (member == null) {
            throw new IllegalStateException("不在该群");
        }
        if (member.getRole() == null || member.getRole() < 1) {
            throw new IllegalStateException("没有权限");
        }
        return member;
    }

    @Transactional(rollbackFor = Exception.class)
    public void quitGroup(Long groupId, Long userId) {
        ChatGroupDO g = chatGroupMapper.selectById(groupId);
        if (g != null && userId.equals(g.getOwnerId())) {
            throw new IllegalStateException("群主请先转让群主或解散群聊");
        }
        saveGroupLog(groupId, "QUIT", userId, null, null, null);
        groupMemberMapper.delete(new LambdaQueryWrapper<ChatGroupMemberDO>()
                .eq(ChatGroupMemberDO::getGroupId, groupId)
                .eq(ChatGroupMemberDO::getUserId, userId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void dissolveGroup(Long groupId, Long userId) {
        ChatGroupDO g = chatGroupMapper.selectById(groupId);
        if (g == null || !userId.equals(g.getOwnerId())) {
            throw new IllegalStateException("仅群主可解散");
        }
        saveGroupLog(groupId, "DISSOLVE", userId, null, null, null);
        g.setStatus(0);
        chatGroupMapper.updateById(g);
    }

    public List<ChatGroupLogVO> groupLogs(Long groupId, Long userId) {
        ChatGroupMemberDO member = groupMemberMapper.selectOne(new LambdaQueryWrapper<ChatGroupMemberDO>()
                .eq(ChatGroupMemberDO::getGroupId, groupId)
                .eq(ChatGroupMemberDO::getUserId, userId));
        if (member == null) {
            throw new IllegalStateException("不在该群");
        }
        List<ChatGroupLogDO> logs = groupLogMapper.selectList(new LambdaQueryWrapper<ChatGroupLogDO>()
                .eq(ChatGroupLogDO::getGroupId, groupId)
                .orderByDesc(ChatGroupLogDO::getCreateTime));
        return logs.stream().map(this::toGroupLogVO).collect(Collectors.toList());
    }

    private void saveGroupLog(Long groupId, String actionType, Long operatorId, Long targetUserId,
            String targetNames, String detail) {
        UserDO operator = userMapper.selectById(operatorId);
        ChatGroupLogDO log = new ChatGroupLogDO();
        log.setGroupId(groupId);
        log.setActionType(actionType);
        log.setOperatorId(operatorId);
        log.setOperatorName(displayUserName(operator));
        log.setTargetUserId(targetUserId);
        if (targetUserId != null) {
            log.setTargetUserName(displayUserName(userMapper.selectById(targetUserId)));
        }
        if (StringUtils.hasText(targetNames)) {
            log.setDetail(targetNames);
        } else if (StringUtils.hasText(detail)) {
            log.setDetail(detail);
        }
        log.setCreateTime(LocalDateTime.now());
        groupLogMapper.insert(log);
    }

    private String displayUserName(UserDO user) {
        if (user == null) {
            return "未知用户";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private ChatGroupLogVO toGroupLogVO(ChatGroupLogDO log) {
        ChatGroupLogVO vo = new ChatGroupLogVO();
        vo.setId(log.getId());
        vo.setActionType(log.getActionType());
        vo.setOperatorId(log.getOperatorId());
        vo.setOperatorName(log.getOperatorName());
        vo.setTargetUserId(log.getTargetUserId());
        vo.setTargetUserName(log.getTargetUserName());
        vo.setDetail(log.getDetail());
        vo.setCreateTime(log.getCreateTime());
        vo.setContent(buildGroupLogContent(log));
        return vo;
    }

    private String buildGroupLogContent(ChatGroupLogDO log) {
        String op = StringUtils.hasText(log.getOperatorName()) ? log.getOperatorName() : "未知用户";
        String target = StringUtils.hasText(log.getTargetUserName()) ? log.getTargetUserName() : "";
        String detail = log.getDetail() != null ? log.getDetail() : "";
        return switch (log.getActionType()) {
            case "CREATE" -> op + " 创建了群组" + (StringUtils.hasText(detail) ? "（" + detail + "）" : "");
            case "INVITE" -> op + " 邀请了 " + detail + " 加入群组";
            case "REMOVE" -> op + " 将 " + target + " 移出了群组";
            case "QUIT" -> op + " 退出了群组";
            case "DISSOLVE" -> op + " 解散了群组";
            case "UPDATE" -> op + " " + detail;
            case "SET_ADMIN" -> op + " 将 " + target + " 设为管理员";
            case "REMOVE_ADMIN" -> op + " 取消了 " + target + " 的管理员身份";
            case "MUTE" -> op + " 禁言了 " + target;
            case "UNMUTE" -> op + " 解除了 " + target + " 的禁言";
            case "TRANSFER_OWNER" -> op + " 将群主转让给 " + target;
            default -> op + " 进行了群操作";
        };
    }
}

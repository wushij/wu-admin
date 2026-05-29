package cn.rbac.server.modules.system.service.message;

import cn.rbac.server.framework.security.core.service.SecurityUtils;
import cn.rbac.server.framework.websocket.MessageWebSocketHandler;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

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
        payload.put("content", msg.getContent());
        payload.put("msgType", msg.getMsgType());
        webSocketHandler.sendChatPayload(receiverId, payload);
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
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserDO u : users) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", u.getId());
            item.put("username", u.getUsername());
            item.put("nickname", u.getNickname());
            item.put("avatar", u.getAvatar());
            item.put("isBlocked", isInMyBlacklist(userId, u.getId()));
            item.put("online", webSocketHandler.isOnline(u.getId()));
            ChatMessageDO latest = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessageDO>()
                    .and(q -> q.and(a -> a.eq(ChatMessageDO::getSenderId, userId).eq(ChatMessageDO::getReceiverId, u.getId()))
                            .or(b -> b.eq(ChatMessageDO::getSenderId, u.getId()).eq(ChatMessageDO::getReceiverId, userId)))
                    .orderByDesc(ChatMessageDO::getSendTime)
                    .last("LIMIT 1"));
            if (latest != null) {
                item.put("lastMessage", latest.getMsgType() != null && latest.getMsgType() == 2 ? "[图片]" : latest.getContent());
                item.put("lastMessageTime", latest.getSendTime());
            }
            long unread = chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessageDO>()
                    .eq(ChatMessageDO::getReceiverId, userId)
                    .eq(ChatMessageDO::getSenderId, u.getId())
                    .eq(ChatMessageDO::getIsRead, 0));
            item.put("unreadCount", unread);
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
        List<Map<String, Object>> list = new ArrayList<>();
        for (ChatGroupMemberDO m : memberships) {
            ChatGroupDO g = chatGroupMapper.selectById(m.getGroupId());
            if (g == null || g.getStatus() == null || g.getStatus() != 1) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("id", g.getId());
            item.put("name", g.getName());
            item.put("ownerId", g.getOwnerId());
            long count = groupMemberMapper.selectCount(new LambdaQueryWrapper<ChatGroupMemberDO>()
                    .eq(ChatGroupMemberDO::getGroupId, g.getId()));
            item.put("memberCount", count);
            ChatGroupMessageDO last = groupMessageMapper.selectOne(new LambdaQueryWrapper<ChatGroupMessageDO>()
                    .eq(ChatGroupMessageDO::getGroupId, g.getId())
                    .orderByDesc(ChatGroupMessageDO::getSendTime)
                    .last("LIMIT 1"));
            if (last != null) {
                item.put("lastMessage", last.getContent());
                item.put("lastMessageTime", last.getSendTime());
            }
            list.add(item);
        }
        return list;
    }

    @Transactional(rollbackFor = Exception.class)
    public ChatGroupMessageDO sendGroupMessage(Long groupId, Long senderId, String content, Integer msgType) {
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
        msg.setMsgType(msgType == null ? 1 : msgType);
        msg.setSendTime(LocalDateTime.now());
        groupMessageMapper.insert(msg);
        List<ChatGroupMemberDO> members = groupMemberMapper.selectList(
                new LambdaQueryWrapper<ChatGroupMemberDO>().eq(ChatGroupMemberDO::getGroupId, groupId));
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", msg.getId());
        payload.put("groupId", groupId);
        payload.put("senderId", senderId);
        payload.put("senderName", msg.getSenderName());
        payload.put("content", content);
        payload.put("msgType", msg.getMsgType());
        for (ChatGroupMemberDO m : members) {
            if (!m.getUserId().equals(senderId)) {
                webSocketHandler.sendGroupChatPayload(m.getUserId(), payload);
            }
        }
        return msg;
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

    @Transactional(rollbackFor = Exception.class)
    public void updateGroup(Long groupId, Long userId, String name, String announcement) {
        ChatGroupMemberDO operator = requireGroupAdmin(groupId, userId);
        if (operator.getRole() == null || operator.getRole() < 1) {
            throw new IllegalStateException("没有权限修改群信息");
        }
        ChatGroupDO group = chatGroupMapper.selectById(groupId);
        if (group == null || group.getStatus() == null || group.getStatus() != 1) {
            throw new IllegalStateException("群不存在或已解散");
        }
        StringBuilder detail = new StringBuilder();
        if (StringUtils.hasText(name) && !name.equals(group.getName())) {
            detail.append("群名称改为「").append(name).append("」");
        }
        if (announcement != null && !announcement.equals(group.getAnnouncement())) {
            if (detail.length() > 0) {
                detail.append("；");
            }
            detail.append("更新了群公告");
        }
        if (StringUtils.hasText(name)) {
            group.setName(name);
        }
        if (announcement != null) {
            group.setAnnouncement(announcement);
        }
        group.setUpdateTime(LocalDateTime.now());
        chatGroupMapper.updateById(group);
        if (detail.length() > 0) {
            saveGroupLog(groupId, "UPDATE", userId, null, null, detail.toString());
        }
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

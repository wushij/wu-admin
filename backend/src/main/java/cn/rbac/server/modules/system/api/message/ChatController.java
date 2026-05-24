package cn.rbac.server.modules.system.api.message;

import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.framework.security.core.service.SecurityUtils;
import cn.rbac.server.framework.websocket.MessageWebSocketHandler;
import cn.rbac.server.modules.system.dal.dataobject.message.ChatGroupDO;
import cn.rbac.server.modules.system.dal.dataobject.message.ChatGroupMessageDO;
import cn.rbac.server.modules.system.dal.dataobject.message.ChatMessageDO;
import cn.rbac.server.modules.system.dal.dataobject.notice.NoticeDO;
import cn.rbac.server.modules.system.dal.mysql.notice.NoticeMapper;
import cn.rbac.server.modules.system.service.message.AnnounceService;
import cn.rbac.server.modules.system.dal.dataobject.file.SysFileDO;
import cn.rbac.server.modules.system.service.message.ChatService;
import cn.rbac.server.modules.system.service.message.vo.ChatGroupLogVO;
import cn.rbac.server.modules.system.service.file.SysFileService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "即时聊天")
@RestController
@RequestMapping("/system/chat")
public class ChatController {

    @Resource
    private ChatService chatService;
    @Resource
    private MessageWebSocketHandler webSocketHandler;
    @Resource
    private SysFileService fileService;

    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    @Operation(summary = "上传聊天图片")
    public CommonResult<SysFileDO> uploadImage(@RequestParam("file") MultipartFile file) {
        return CommonResult.success(fileService.uploadChatImage(file));
    }

    @PostMapping("/send")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    @Operation(summary = "发送私聊")
    public CommonResult<ChatMessageDO> send(@RequestBody SendReq req) {
        return CommonResult.success(chatService.sendPrivate(
                SecurityUtils.getLoginUserId(), req.getReceiverId(), req.getContent(), req.getMsgType()));
    }

    @GetMapping("/history/{targetId}")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<PageResult<ChatMessageDO>> history(@PathVariable Long targetId, PageParam pageParam) {
        Page<ChatMessageDO> page = chatService.history(SecurityUtils.getLoginUserId(), targetId,
                pageParam.getPageNo(), pageParam.getPageSize());
        return CommonResult.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @GetMapping("/users")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<List<Map<String, Object>>> users() {
        return CommonResult.success(chatService.listUsers(SecurityUtils.getLoginUserId()));
    }

    @PostMapping("/read/{senderId}")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<Boolean> read(@PathVariable Long senderId) {
        chatService.markRead(SecurityUtils.getLoginUserId(), senderId);
        return CommonResult.success(true);
    }

    @GetMapping("/unread-count")
    public CommonResult<Long> unreadCount() {
        return CommonResult.success(chatService.unreadCount(SecurityUtils.getLoginUserId()));
    }

    @GetMapping("/online/{userId}")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<Boolean> online(@PathVariable Long userId) {
        return CommonResult.success(webSocketHandler.isOnline(userId));
    }

    @DeleteMapping("/clear/{targetId}")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<Boolean> clear(@PathVariable Long targetId) {
        chatService.clearHistory(SecurityUtils.getLoginUserId(), targetId);
        return CommonResult.success(true);
    }

    @PostMapping("/block/{targetId}")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<Boolean> block(@PathVariable Long targetId) {
        chatService.block(SecurityUtils.getLoginUserId(), targetId);
        return CommonResult.success(true);
    }

    @DeleteMapping("/block/{targetId}")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<Boolean> unblock(@PathVariable Long targetId) {
        chatService.unblock(SecurityUtils.getLoginUserId(), targetId);
        return CommonResult.success(true);
    }

    @Data
    public static class SendReq {
        private Long receiverId;
        private String content;
        private Integer msgType;
    }
}

@Tag(name = "群聊")
@RestController
@RequestMapping("/system/chat/group")
class ChatGroupController {

    @Resource
    private ChatService chatService;

    @GetMapping("/{groupId}")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<ChatGroupDO> detail(@PathVariable Long groupId) {
        return CommonResult.success(chatService.getGroupDetail(groupId));
    }

    @PutMapping("/update")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<Boolean> update(@RequestBody UpdateGroupReq req) {
        chatService.updateGroup(req.getId(), SecurityUtils.getLoginUserId(), req.getName(), req.getAnnouncement());
        return CommonResult.success(true);
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<ChatGroupDO> create(@RequestBody CreateGroupReq req) {
        return CommonResult.success(chatService.createGroup(
                SecurityUtils.getLoginUserId(), req.getName(), req.getMemberIds()));
    }

    @GetMapping("/list")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<List<Map<String, Object>>> list() {
        return CommonResult.success(chatService.myGroups(SecurityUtils.getLoginUserId()));
    }

    @PostMapping("/{groupId}/message")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<ChatGroupMessageDO> send(@PathVariable Long groupId, @RequestBody ChatController.SendReq req) {
        return CommonResult.success(chatService.sendGroupMessage(
                groupId, SecurityUtils.getLoginUserId(), req.getContent(), req.getMsgType()));
    }

    @GetMapping("/{groupId}/messages")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<PageResult<ChatGroupMessageDO>> messages(@PathVariable Long groupId, PageParam pageParam) {
        Page<ChatGroupMessageDO> page = chatService.groupMessages(groupId, SecurityUtils.getLoginUserId(),
                pageParam.getPageNo(), pageParam.getPageSize());
        return CommonResult.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @GetMapping("/{groupId}/members")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<List<Map<String, Object>>> members(@PathVariable Long groupId) {
        return CommonResult.success(chatService.groupMembers(groupId));
    }

    @GetMapping("/{groupId}/logs")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    @Operation(summary = "群聊操作日志")
    public CommonResult<List<ChatGroupLogVO>> logs(@PathVariable Long groupId) {
        return CommonResult.success(chatService.groupLogs(groupId, SecurityUtils.getLoginUserId()));
    }

    @PostMapping("/{groupId}/members")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<Boolean> addMembers(@PathVariable Long groupId, @RequestBody MemberIdsReq req) {
        chatService.addMembers(groupId, req.getUserIds(), SecurityUtils.getLoginUserId());
        return CommonResult.success(true);
    }

    @DeleteMapping("/{groupId}/members/{memberUserId}")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<Boolean> removeMember(@PathVariable Long groupId, @PathVariable Long memberUserId) {
        chatService.removeMember(groupId, memberUserId, SecurityUtils.getLoginUserId());
        return CommonResult.success(true);
    }

    @PostMapping("/{groupId}/admin/{memberUserId}")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<Boolean> setAdmin(@PathVariable Long groupId, @PathVariable Long memberUserId,
            @RequestParam(defaultValue = "true") boolean admin) {
        chatService.setAdmin(groupId, memberUserId, SecurityUtils.getLoginUserId(), admin);
        return CommonResult.success(true);
    }

    @PostMapping("/{groupId}/mute/{memberUserId}")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<Boolean> setMuted(@PathVariable Long groupId, @PathVariable Long memberUserId,
            @RequestParam(defaultValue = "true") boolean muted) {
        chatService.setMuted(groupId, memberUserId, SecurityUtils.getLoginUserId(), muted);
        return CommonResult.success(true);
    }

    @PostMapping("/{groupId}/transfer/{newOwnerId}")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<Boolean> transferOwner(@PathVariable Long groupId, @PathVariable Long newOwnerId) {
        chatService.transferOwner(groupId, newOwnerId, SecurityUtils.getLoginUserId());
        return CommonResult.success(true);
    }

    @PostMapping("/{groupId}/quit")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<Boolean> quit(@PathVariable Long groupId) {
        chatService.quitGroup(groupId, SecurityUtils.getLoginUserId());
        return CommonResult.success(true);
    }

    @DeleteMapping("/{groupId}")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<Boolean> dissolve(@PathVariable Long groupId) {
        chatService.dissolveGroup(groupId, SecurityUtils.getLoginUserId());
        return CommonResult.success(true);
    }

    @Data
    public static class CreateGroupReq {
        private String name;
        private List<Long> memberIds;
    }

    @Data
    public static class UpdateGroupReq {
        private Long id;
        private String name;
        private String announcement;
    }

    @Data
    public static class MemberIdsReq {
        private List<Long> userIds;
    }
}

@Tag(name = "消息汇总")
@RestController
@RequestMapping("/system/message")
class MessageCenterController {

    @Resource
    private AnnounceService announceService;
    @Resource
    private ChatService chatService;
    @Resource
    private NoticeMapper noticeMapper;

    @GetMapping("/summary")
    @Operation(summary = "未读汇总")
    public CommonResult<Map<String, Long>> summary() {
        Long userId = SecurityUtils.getLoginUserId();
        long inbox = noticeMapper.selectCount(new LambdaQueryWrapper<NoticeDO>()
                .eq(NoticeDO::getUserId, userId)
                .eq(NoticeDO::getReadStatus, 0));
        long announce = announceService.unreadCount(userId);
        long chat = chatService.unreadCount(userId);
        Map<String, Long> map = new HashMap<>();
        map.put("inboxCount", inbox);
        map.put("announceCount", announce);
        map.put("chatCount", chat);
        map.put("noticeCount", inbox + announce);
        map.put("total", inbox + announce + chat);
        return CommonResult.success(map);
    }
}

package cn.rbac.server.modules.system.api.message;

import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.framework.security.core.service.SecurityUtils;
import cn.rbac.server.framework.websocket.MessageWebSocketHandler;
import cn.rbac.server.modules.system.dal.dataobject.message.ChatGroupDO;
import cn.rbac.server.modules.system.dal.dataobject.message.ChatGroupMessageDO;
import cn.rbac.server.modules.system.dal.dataobject.message.ChatMessageDO;
import cn.rbac.server.modules.system.service.message.AnnounceService;
import cn.rbac.server.modules.system.service.message.NoticeService;
import cn.rbac.server.modules.system.dal.dataobject.file.SysFileDO;
import cn.rbac.server.modules.system.service.message.ChatService;
import cn.rbac.server.modules.system.service.message.vo.ChatGroupLogVO;
import cn.rbac.server.modules.system.service.file.SysFileService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "企业IM")
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

    @PostMapping(value = "/upload/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    @Operation(summary = "上传聊天文件")
    public CommonResult<SysFileDO> uploadFile(@RequestParam("file") MultipartFile file) {
        return CommonResult.success(fileService.uploadChatFile(file));
    }

    @GetMapping("/can-create-group")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    @Operation(summary = "是否可创建群聊")
    public CommonResult<Boolean> canCreateGroup() {
        return CommonResult.success(chatService.canCreateGroup(SecurityUtils.getLoginUserId()));
    }

    @PostMapping("/recall/{messageId}")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    @Operation(summary = "撤回私聊消息")
    public CommonResult<ChatMessageDO> recallPrivate(@PathVariable Long messageId) {
        return CommonResult.success(chatService.recallPrivate(SecurityUtils.getLoginUserId(), messageId));
    }

    @PostMapping("/typing/{targetUserId}")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    @Operation(summary = "私聊正在输入")
    public CommonResult<Boolean> typing(@PathVariable Long targetUserId) {
        chatService.relayTyping(SecurityUtils.getLoginUserId(), targetUserId);
        return CommonResult.success(true);
    }

    @PostMapping("/send")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    @Operation(summary = "发送私聊")
    public CommonResult<ChatMessageDO> send(@Validated @RequestBody SendReq req) {
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
    @PreAuthorize("@ss.hasRead('system:chat:list')")
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
        @NotNull(message = "接收者ID不能为空")
        private Long receiverId;
        @NotBlank(message = "消息内容不能为空")
        private String content;
        private Integer msgType;
        private List<Long> mentionIds;
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
    public CommonResult<Map<String, Object>> detail(@PathVariable Long groupId) {
        return CommonResult.success(chatService.getGroupContext(groupId, SecurityUtils.getLoginUserId()));
    }

    @PostMapping("/{groupId}/announcement/read")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    @Operation(summary = "标记群公告已读（收起置顶）")
    public CommonResult<Boolean> readAnnouncement(@PathVariable Long groupId) {
        chatService.markAnnouncementRead(groupId, SecurityUtils.getLoginUserId());
        return CommonResult.success(true);
    }

    @PostMapping("/{groupId}/notify-muted")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    @Operation(summary = "设置本群免打扰（仅 @ 我时提醒）")
    public CommonResult<Boolean> setNotifyMuted(@PathVariable Long groupId,
            @RequestParam(defaultValue = "true") boolean muted) {
        chatService.setNotifyMuted(groupId, SecurityUtils.getLoginUserId(), muted);
        return CommonResult.success(true);
    }

    @PutMapping("/update")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<Boolean> update(@Validated @RequestBody UpdateGroupReq req) {
        chatService.updateGroup(req.getId(), SecurityUtils.getLoginUserId(), req.getName(), req.getAnnouncement());
        return CommonResult.success(true);
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    public CommonResult<ChatGroupDO> create(@Validated @RequestBody CreateGroupReq req) {
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
    public CommonResult<ChatGroupMessageDO> send(@PathVariable Long groupId, @Validated @RequestBody GroupSendReq req) {
        return CommonResult.success(chatService.sendGroupMessage(
                groupId, SecurityUtils.getLoginUserId(), req.getContent(), req.getMsgType(), req.getMentionIds()));
    }

    @PostMapping("/{groupId}/message/{messageId}/recall")
    @PreAuthorize("@ss.hasRead('system:chat:list')")
    @Operation(summary = "撤回群消息")
    public CommonResult<ChatGroupMessageDO> recallGroupMessage(@PathVariable Long groupId,
            @PathVariable Long messageId) {
        return CommonResult.success(chatService.recallGroupMessage(
                groupId, SecurityUtils.getLoginUserId(), messageId));
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
    public CommonResult<Boolean> addMembers(@PathVariable Long groupId, @Validated @RequestBody MemberIdsReq req) {
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
        @NotBlank(message = "群名称不能为空")
        @Size(max = 50, message = "群名称最多 50 个字符")
        private String name;
        private List<Long> memberIds;
    }

    @Data
    public static class UpdateGroupReq {
        @NotNull(message = "群ID不能为空")
        private Long id;
        @NotBlank(message = "群名称不能为空")
        @Size(max = 50, message = "群名称最多 50 个字符")
        private String name;
        private String announcement;
    }

    @Data
    public static class MemberIdsReq {
        @NotNull(message = "成员ID不能为空")
        private List<Long> userIds;
    }

    /** 群消息发送（无 receiverId，与私聊 SendReq 区分） */
    @Data
    public static class GroupSendReq {
        @NotBlank(message = "消息内容不能为空")
        private String content;
        private Integer msgType;
        private List<Long> mentionIds;
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
    private NoticeService noticeService;

    @GetMapping("/summary")
    @Operation(summary = "未读汇总")
    public CommonResult<Map<String, Long>> summary() {
        Long userId = SecurityUtils.getLoginUserId();
        long inbox = noticeService.unreadCount(userId);
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

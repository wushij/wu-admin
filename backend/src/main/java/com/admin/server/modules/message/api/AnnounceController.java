package com.admin.server.modules.message.api;

import com.admin.server.common.core.CommonResult;
import com.admin.server.common.core.PageParam;
import com.admin.server.common.core.PageResult;
import com.admin.server.framework.security.core.service.SecurityUtils;
import com.admin.server.modules.message.dal.dataobject.AnnounceDO;
import com.admin.server.modules.message.dal.dataobject.AnnounceSendLogDO;
import com.admin.server.modules.message.service.AnnounceService;
import com.admin.server.modules.message.service.vo.AnnounceMyVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.admin.server.framework.log.annotation.Log;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "系统通知")
@Slf4j
@RestController
@RequestMapping("/system/announce")
public class AnnounceController {

    @Resource
    private AnnounceService announceService;
    @Resource
    private ObjectMapper objectMapper;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasRead('system:announce:list')")
    @Operation(summary = "通知分页")
    public CommonResult<PageResult<AnnounceDO>> page(PageParam pageParam,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer noticeType,
            @RequestParam(required = false) Integer status) {
        Page<AnnounceDO> page = announceService.page(pageParam.getPageNo(), pageParam.getPageSize(), title, noticeType,
                status, SecurityUtils.getLoginUserIdOrZero());
        return CommonResult.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @GetMapping("/my")
    @Operation(summary = "我的通知")
    public CommonResult<PageResult<AnnounceMyVO>> my(PageParam pageParam,
            @RequestParam(required = false) Integer isRead) {
        Long userId = SecurityUtils.getLoginUserId();
        Page<AnnounceMyVO> page = announceService.myPage(userId, pageParam.getPageNo(), pageParam.getPageSize(), isRead);
        return CommonResult.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "通知详情")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<AnnounceRequest> detail(@PathVariable Long id) {
        // 可见性由 service 判定：通知管理/发布人可看草稿，其余用户仅可见「已发布且已投递给自己」的通知
        AnnounceDO entity = announceService.getVisibleById(id, SecurityUtils.getLoginUserIdOrZero());
        AnnounceRequest resp = AnnounceRequest.from(entity, objectMapper);
        resp.setCreateTime(entity.getCreateTime());
        resp.setCreateName(announceService.resolvePublisherName(entity));
        resp.setCreateAvatar(announceService.resolvePublisherAvatar(entity));
        return CommonResult.success(resp);
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermission('system:announce:create')")
    @Operation(summary = "新增通知")
    @Log(title = "通知公告", businessType = Log.BusinessType.INSERT)
    public CommonResult<Boolean> create(@RequestBody AnnounceRequest req) {
        announceService.create(req.toEntity(objectMapper));
        return CommonResult.success(true);
    }

    @PutMapping
    @PreAuthorize("@ss.hasPermission('system:announce:update')")
    @Operation(summary = "修改通知")
    @Log(title = "通知公告", businessType = Log.BusinessType.UPDATE)
    public CommonResult<Boolean> update(@RequestBody AnnounceRequest req) {
        announceService.update(req.toEntity(objectMapper));
        return CommonResult.success(true);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermission('system:announce:delete')")
    @Operation(summary = "删除通知")
    @Log(title = "通知公告", businessType = Log.BusinessType.DELETE)
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        announceService.delete(id);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("@ss.hasPermission('system:announce:publish')")
    @Operation(summary = "发布通知")
    @Log(title = "通知公告", businessType = Log.BusinessType.OTHER)
    public CommonResult<Boolean> publish(@PathVariable Long id) {
        announceService.publish(id);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记已读")
    public CommonResult<Boolean> read(@PathVariable Long id) {
        announceService.markRead(SecurityUtils.getLoginUserId(), id);
        return CommonResult.success(true);
    }

    @PostMapping("/read-all")
    @Operation(summary = "全部已读")
    public CommonResult<Boolean> readAll() {
        announceService.markAllRead(SecurityUtils.getLoginUserId());
        return CommonResult.success(true);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "未读数量")
    public CommonResult<Long> unreadCount() {
        return CommonResult.success(announceService.unreadCount(SecurityUtils.getLoginUserId()));
    }

    @GetMapping("/{id}/send-logs")
    @PreAuthorize("@ss.hasRead('system:announce:list')")
    @Operation(summary = "发送日志")
    public CommonResult<List<AnnounceSendLogDO>> sendLogs(@PathVariable Long id) {
        return CommonResult.success(announceService.sendLogs(id, SecurityUtils.getLoginUserIdOrZero()));
    }

    @GetMapping("/recycle/page")
    @PreAuthorize("@ss.hasRecycleRead()")
    @Operation(summary = "回收站分页")
    public CommonResult<PageResult<AnnounceDO>> recyclePage(PageParam pageParam,
            @RequestParam(required = false) String title) {
        return CommonResult.success(announceService.recyclePage(pageParam, title));
    }

    @PutMapping("/restore")
    @PreAuthorize("@ss.hasRecycleRestore('system:announce:delete')")
    @Operation(summary = "恢复通知")
    @Log(title = "通知公告", businessType = Log.BusinessType.OTHER)
    public CommonResult<Boolean> restore(@RequestParam Long id) {
        announceService.restore(id);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete-permanent")
    @PreAuthorize("@ss.hasRecycleDelete('system:announce:delete')")
    @Operation(summary = "彻底删除")
    @Log(title = "通知公告", businessType = Log.BusinessType.DELETE)
    public CommonResult<Boolean> deletePermanent(@RequestParam Long id) {
        announceService.deletePermanent(id);
        return CommonResult.success(true);
    }

    @Data
    public static class AnnounceRequest {
        private Long id;
        private String title;
        private String content;
        private Integer noticeType;
        private List<String> channels;
        private Integer targetType;
        private List<Long> targetIds;
        private Integer status;
        private String createName;
        private LocalDateTime createTime;
        private String createAvatar;

        static AnnounceRequest from(AnnounceDO e, ObjectMapper mapper) {
            AnnounceRequest r = new AnnounceRequest();
            r.setId(e.getId());
            r.setTitle(e.getTitle());
            r.setContent(e.getContent());
            r.setNoticeType(e.getNoticeType());
            r.setTargetType(e.getTargetType());
            r.setStatus(e.getStatus());
            try {
                if (StringUtils.hasText(e.getChannels())) {
                    r.setChannels(mapper.readValue(e.getChannels(), new TypeReference<List<String>>() {}));
                }
                if (StringUtils.hasText(e.getTargetIds())) {
                    r.setTargetIds(mapper.readValue(e.getTargetIds(), new TypeReference<List<Long>>() {}));
                }
            } catch (Exception ex) {
                log.warn("解析通知 channels/targetIds 失败 announceId={}", e.getId(), ex);
            }
            return r;
        }

        AnnounceDO toEntity(ObjectMapper mapper) {
            AnnounceDO e = new AnnounceDO();
            e.setId(id);
            e.setTitle(title);
            e.setContent(content);
            e.setNoticeType(noticeType);
            e.setTargetType(targetType);
            e.setStatus(status);
            try {
                e.setChannels(channels == null ? "[\"station\"]" : mapper.writeValueAsString(channels));
                e.setTargetIds(targetIds == null ? null : mapper.writeValueAsString(targetIds));
            } catch (Exception ex) {
                log.warn("序列化通知 channels/targetIds 失败 id={}", id, ex);
                e.setChannels("[\"station\"]");
            }
            return e;
        }
    }
}

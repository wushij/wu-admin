package cn.rbac.server.modules.system.api.message;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.framework.security.core.service.SecurityUtils;
import cn.rbac.server.modules.system.dal.dataobject.message.AnnounceDO;
import cn.rbac.server.modules.system.dal.dataobject.message.AnnounceSendLogDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.service.message.AnnounceService;
import cn.rbac.server.modules.system.service.message.vo.AnnounceMyVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "????")
@RestController
@RequestMapping("/system/announce")
public class AnnounceController {

    @Resource
    private AnnounceService announceService;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private UserMapper userMapper;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasRead('system:announce:list')")
    @Operation(summary = "????")
    public CommonResult<PageResult<AnnounceDO>> page(PageParam pageParam,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer noticeType,
            @RequestParam(required = false) Integer status) {
        Page<AnnounceDO> page = announceService.page(pageParam.getPageNo(), pageParam.getPageSize(), title, noticeType, status);
        return CommonResult.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @GetMapping("/my")
    @Operation(summary = "????")
    public CommonResult<PageResult<AnnounceMyVO>> my(PageParam pageParam,
            @RequestParam(required = false) Integer isRead) {
        Long userId = SecurityUtils.getLoginUserId();
        Page<AnnounceMyVO> page = announceService.myPage(userId, pageParam.getPageNo(), pageParam.getPageSize(), isRead);
        return CommonResult.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "????")
    public CommonResult<AnnounceRequest> detail(@PathVariable Long id) {
        AnnounceDO entity = announceService.getById(id);
        if (entity == null) {
            throw new BusinessException(404, "?????");
        }
        AnnounceRequest resp = AnnounceRequest.from(entity, objectMapper);
        resp.setCreateName(entity.getCreateName());
        resp.setCreateTime(entity.getCreateTime());
        if (entity.getCreateBy() != null) {
            UserDO publisher = userMapper.selectById(entity.getCreateBy());
            if (publisher != null) {
                resp.setCreateAvatar(publisher.getAvatar());
                if (!StringUtils.hasText(resp.getCreateName())) {
                    resp.setCreateName(publisher.getNickname() != null ? publisher.getNickname() : publisher.getUsername());
                }
            }
        }
        return CommonResult.success(resp);
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermission('system:announce:create')")
    @Operation(summary = "????")
    public CommonResult<Boolean> create(@RequestBody AnnounceRequest req) {
        announceService.create(req.toEntity(objectMapper));
        return CommonResult.success(true);
    }

    @PutMapping
    @PreAuthorize("@ss.hasPermission('system:announce:update')")
    @Operation(summary = "????")
    public CommonResult<Boolean> update(@RequestBody AnnounceRequest req) {
        announceService.update(req.toEntity(objectMapper));
        return CommonResult.success(true);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermission('system:announce:delete')")
    @Operation(summary = "????")
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        announceService.delete(id);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("@ss.hasPermission('system:announce:publish')")
    @Operation(summary = "????")
    public CommonResult<Boolean> publish(@PathVariable Long id) {
        announceService.publish(id);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "????")
    public CommonResult<Boolean> read(@PathVariable Long id) {
        announceService.markRead(SecurityUtils.getLoginUserId(), id);
        return CommonResult.success(true);
    }

    @PostMapping("/read-all")
    @Operation(summary = "????")
    public CommonResult<Boolean> readAll() {
        announceService.markAllRead(SecurityUtils.getLoginUserId());
        return CommonResult.success(true);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "???")
    public CommonResult<Long> unreadCount() {
        return CommonResult.success(announceService.unreadCount(SecurityUtils.getLoginUserId()));
    }

    @GetMapping("/{id}/send-logs")
    @PreAuthorize("@ss.hasRead('system:announce:list')")
    @Operation(summary = "????")
    public CommonResult<List<AnnounceSendLogDO>> sendLogs(@PathVariable Long id) {
        return CommonResult.success(announceService.sendLogs(id));
    }

    @GetMapping("/recycle/page")
    @PreAuthorize("@ss.hasPermission('system:announce:delete')")
    @Operation(summary = "?????")
    public CommonResult<PageResult<AnnounceDO>> recyclePage(PageParam pageParam,
            @RequestParam(required = false) String title) {
        return CommonResult.success(announceService.recyclePage(pageParam, title));
    }

    @PutMapping("/restore")
    @PreAuthorize("@ss.hasPermission('system:announce:delete')")
    @Operation(summary = "????")
    public CommonResult<Boolean> restore(@RequestParam Long id) {
        announceService.restore(id);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete-permanent")
    @PreAuthorize("@ss.hasPermission('system:announce:delete')")
    @Operation(summary = "????")
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
            } catch (Exception ignored) {
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
                e.setChannels("[\"station\"]");
            }
            return e;
        }
    }
}

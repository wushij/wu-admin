package com.admin.server.modules.system.api.notice;

import com.admin.server.common.pojo.CommonResult;
import com.admin.server.framework.security.core.service.SecurityUtils;
import com.admin.server.modules.system.dal.dataobject.notice.NoticeDO;
import com.admin.server.modules.system.service.notice.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "站内消息")
@RestController
@RequestMapping("/system/notice")
public class NoticeController {

    @Resource
    private NoticeService noticeService;

    @Operation(summary = "我的未读数")
    @GetMapping("/unread-count")
    public CommonResult<Long> unreadCount() {
        return CommonResult.success(noticeService.unreadCount(SecurityUtils.getLoginUserIdOrZero()));
    }

    @Operation(summary = "我的消息列表")
    @GetMapping("/my-list")
    public CommonResult<List<NoticeDO>> myList() {
        return CommonResult.success(noticeService.myList(SecurityUtils.getLoginUserIdOrZero()));
    }

    @Operation(summary = "标记消息已读")
    @PutMapping("/read")
    public CommonResult<Boolean> read(@RequestBody NoticeReadReqVO reqVO) {
        noticeService.markRead(SecurityUtils.getLoginUserIdOrZero(), reqVO.getId());
        return CommonResult.success(true);
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public CommonResult<Boolean> readAll() {
        noticeService.markAllRead(SecurityUtils.getLoginUserIdOrZero());
        return CommonResult.success(true);
    }

    @Data
    public static class NoticeReadReqVO {
        private Long id;
    }
}

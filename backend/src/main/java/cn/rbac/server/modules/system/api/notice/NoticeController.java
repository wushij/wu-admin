package cn.rbac.server.modules.system.api.notice;

import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.framework.security.core.service.SecurityUtils;
import cn.rbac.server.modules.system.dal.dataobject.notice.NoticeDO;
import cn.rbac.server.modules.system.dal.mysql.notice.NoticeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

@Tag(name = "站内消息")
@RestController
@RequestMapping("/system/notice")
public class NoticeController {

    @Resource
    private NoticeMapper noticeMapper;

    @Operation(summary = "我的未读数")
    @GetMapping("/unread-count")
    public CommonResult<Long> unreadCount() {
        long count = noticeMapper.selectCount(new LambdaQueryWrapper<NoticeDO>()
                .eq(NoticeDO::getUserId, SecurityUtils.getLoginUserIdOrZero())
                .eq(NoticeDO::getReadStatus, 0)
                .orderByDesc(NoticeDO::getCreateTime));
        return CommonResult.success(count);
    }

    @Operation(summary = "我的消息列表")
    @GetMapping("/my-list")
    public CommonResult<List<NoticeDO>> myList() {
        List<NoticeDO> list = noticeMapper.selectList(new LambdaQueryWrapper<NoticeDO>()
                .eq(NoticeDO::getUserId, SecurityUtils.getLoginUserIdOrZero())
                .orderByDesc(NoticeDO::getCreateTime)
                .last("limit 20"));
        return CommonResult.success(list);
    }

    @Operation(summary = "标记消息已读")
    @PutMapping("/read")
    public CommonResult<Boolean> read(@RequestBody NoticeReadReqVO reqVO) {
        NoticeDO notice = noticeMapper.selectById(reqVO.getId());
        if (notice == null || !SecurityUtils.getLoginUserIdOrZero().equals(notice.getUserId())) {
            return CommonResult.error(404, "消息不存在");
        }
        notice.setReadStatus(1);
        noticeMapper.updateById(notice);
        return CommonResult.success(true);
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public CommonResult<Boolean> readAll() {
        noticeMapper.markAllReadByUserId(SecurityUtils.getLoginUserIdOrZero());
        return CommonResult.success(true);
    }

    @Data
    public static class NoticeReadReqVO {
        private Long id;
    }
}

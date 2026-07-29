package com.admin.server.modules.ai.api;

import com.admin.server.common.core.CommonResult;
import com.admin.server.framework.security.core.service.SecurityUtils;
import com.admin.server.modules.ai.api.vo.AiChatHistoryItemVO;
import com.admin.server.modules.ai.api.vo.AiChatStreamReqVO;
import com.admin.server.modules.ai.api.vo.AiConversationVO;
import com.admin.server.modules.ai.api.vo.AiModelRespVO;
import com.admin.server.modules.ai.service.AiChatLogService;
import com.admin.server.modules.ai.service.AiChatService;
import com.admin.server.modules.ai.service.AiModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * AI wu助手对话 API（登录即可使用，无需菜单权限）
 */
@Tag(name = "AI wu助手对话")
@RestController
@RequestMapping("/ai/chat")
public class AiChatController {

    @Resource
    private AiChatService aiChatService;

    @Resource
    private AiModelService aiModelService;

    @Resource
    private AiChatLogService aiChatLogService;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式对话（SSE 增量推送，事件: delta/done/error）")
    public SseEmitter stream(@RequestBody AiChatStreamReqVO reqVO) {
        Long userId = SecurityUtils.getLoginUserIdOrZero();
        String username = SecurityUtils.getLoginUsername();
        return aiChatService.stream(reqVO, userId, username);
    }

    @GetMapping("/models")
    @Operation(summary = "启用中的模型列表（悬浮窗模型选择器）")
    public CommonResult<List<AiModelRespVO>> models() {
        return CommonResult.success(aiModelService.listEnabled());
    }

    @GetMapping("/conversations")
    @Operation(summary = "我的历史会话列表（仅本人，最近优先）")
    public CommonResult<List<AiConversationVO>> conversations() {
        Long userId = SecurityUtils.getLoginUserIdOrZero();
        return CommonResult.success(aiChatLogService.listConversations(userId));
    }

    @GetMapping("/history")
    @Operation(summary = "指定会话的问答序列（仅本人，用于恢复续聊）")
    public CommonResult<List<AiChatHistoryItemVO>> history(@RequestParam String conversationId) {
        Long userId = SecurityUtils.getLoginUserIdOrZero();
        return CommonResult.success(aiChatLogService.listHistory(userId, conversationId));
    }
}

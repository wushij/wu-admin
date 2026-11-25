package com.admin.server.modules.ai.tool;

import cn.hutool.core.util.StrUtil;
import com.admin.server.modules.ai.service.search.vo.WebSearchCitationVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具执行上下文
 * <p>
 * 身份信息与联网开关一律由服务端从会话/请求注入，工具内部只能使用此处的 userId/username/webSearchEnabled，
 * 严禁信任大模型传入的用户标识参数，从根上杜绝横向越权（设计方案 §5.2）。
 * 同时用于回收集成工具（如 web_search）产出的参考外链，以便流式期间实时下发前端与落库保存。
 * </p>
 */
public class AiToolContext {

    private final Long userId;
    private final String username;
    private final boolean webSearchEnabled;
    private final Map<String, WebSearchCitationVO> citationsByUrl = new LinkedHashMap<>();

    public AiToolContext(Long userId, String username) {
        this(userId, username, false);
    }

    public AiToolContext(Long userId, String username, boolean webSearchEnabled) {
        this.userId = userId;
        this.username = username;
        this.webSearchEnabled = webSearchEnabled;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public boolean isWebSearchEnabled() {
        return webSearchEnabled;
    }

    /** 追加本轮工具产出的有效外链引用（按 URL 去重，保留顺序） */
    public synchronized void addCitations(List<WebSearchCitationVO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (WebSearchCitationVO item : items) {
            if (item == null || StrUtil.isBlank(item.getUrl())) {
                continue;
            }
            citationsByUrl.putIfAbsent(item.getUrl().trim(), item);
        }
    }

    /** 获取当前已累积的全部去重引用快照 */
    public synchronized List<WebSearchCitationVO> getCitationsSnapshot() {
        return new ArrayList<>(citationsByUrl.values());
    }
}

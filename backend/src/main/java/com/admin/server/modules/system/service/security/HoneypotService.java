package com.admin.server.modules.system.service.security;

/**
 * 蜜罐诱捕服务。
 *
 * <p>蜜罐只负责「记录 + 处置 + 投喂诱饵」，不返回任何真实业务数据。
 * 关闭时由入口返回 404，与真实不存在的路径保持一致，避免暴露蜜罐的存在。</p>
 */
public interface HoneypotService {

    /**
     * 蜜罐是否启用（来自系统配置，默认关闭）。
     */
    boolean isEnabled();

    /**
     * 记录一次蜜罐命中并生成诱饵响应。
     *
     * @param request HTTP 请求
     * @param trapId  诱饵标识
     * @return 诱饵响应内容
     */
    HoneypotResult hit(jakarta.servlet.http.HttpServletRequest request, String trapId);
}

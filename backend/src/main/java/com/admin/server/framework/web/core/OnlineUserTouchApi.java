package com.admin.server.framework.web.core;

/**
 * 在线用户活跃心跳上报 SPI（框架层定义，业务层 OnlineUserService 实现）
 */
public interface OnlineUserTouchApi {

    /**
     * 刷新用户最后活跃时间
     *
     * @param userId 用户 ID
     */
    void touchLastAccess(Long userId);
}

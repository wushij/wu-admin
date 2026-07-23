package com.admin.server.modules.system.service.monitor;

import com.admin.server.modules.system.api.monitor.vo.OnlineUserVO;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public interface OnlineUserService {

    void recordLoginSession(Long userId, String username, String nickname, HttpServletRequest request);

    void recordLoginSession(Long userId, String username, String nickname, String clientIp, String userAgent);

    void touchLastAccess(Long userId);

    /** 工作台等场景仅需人数，避免构建完整在线用户 VO 列表 */
    int countOnlineUsers();

    List<OnlineUserVO> listOnlineUsers();

    void forceLogout(Long userId);
}

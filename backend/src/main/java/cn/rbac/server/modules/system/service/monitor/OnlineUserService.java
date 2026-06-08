package cn.rbac.server.modules.system.service.monitor;

import cn.rbac.server.modules.system.api.monitor.vo.OnlineUserVO;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public interface OnlineUserService {

    void recordLoginSession(Long userId, String username, String nickname, HttpServletRequest request);

    void recordLoginSession(Long userId, String username, String nickname, String clientIp, String userAgent);

    void touchLastAccess(Long userId);

    List<OnlineUserVO> listOnlineUsers();

    void forceLogout(Long userId);
}

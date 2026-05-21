package cn.rbac.server.modules.system.service.monitor;

import cn.rbac.server.modules.system.controller.admin.monitor.vo.OnlineUserVO;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public interface OnlineUserService {

    void recordLoginSession(Long userId, String username, String nickname, HttpServletRequest request);

    void touchLastAccess(Long userId);

    List<OnlineUserVO> listOnlineUsers();

    void forceLogout(Long userId);
}

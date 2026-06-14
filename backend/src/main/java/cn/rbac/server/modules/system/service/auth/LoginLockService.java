package cn.rbac.server.modules.system.service.auth;

import cn.rbac.server.modules.system.service.auth.vo.LoginLockStatusVO;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface LoginLockService {

    /** 登录前检查：账号或 IP 是否被临时锁定，返回提示文案；未锁定返回 null */
    String checkLoginLockMessage(String username, String ip);

    /** 登录失败：累计失败次数，达阈值则分别锁定账号与 IP */
    void recordLoginFailure(String username, String ip);

    /** 登录成功：清除该次登录 username + ip 的失败与锁定 */
    void clearLoginFailure(String username, String ip);

    /** 管理端：查询指定用户名的登录锁定状态 */
    LoginLockStatusVO getUserLockStatus(String username);

    /** 管理端：批量查询（用户列表展示用） */
    Map<String, LoginLockStatusVO> getUserLockStatusMap(Collection<String> usernames);

    /** 管理端：查询指定 IP 的登录锁定状态 */
    LoginLockStatusVO getIpLockStatus(String ip);

    /** 管理端：批量查询 IP 锁定状态 */
    Map<String, LoginLockStatusVO> getIpLockStatusMap(Collection<String> ips);

    /** 管理端：解除指定用户名的登录锁定（账号侧 fail/lock） */
    void unlockUser(String username);

    /** 管理端：解除指定 IP 的登录锁定（IP 侧 fail/lock） */
    void unlockIp(String ip);

    /** 管理端：当前处于账号锁定状态的用户名集合 */
    Set<String> listLockedUsernames();

    /** 管理端：当前处于 IP 锁定状态的地址集合 */
    Set<String> listLockedIps();
}

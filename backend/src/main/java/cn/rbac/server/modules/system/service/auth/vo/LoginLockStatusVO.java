package cn.rbac.server.modules.system.service.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 登录失败锁定状态（Redis，非数据库账号停用） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginLockStatusVO {
    /** 是否处于登录锁定中 */
    private boolean locked;
    /** 锁定剩余秒数；未锁定时为 0 */
    private long remainSeconds;
    /** 当前累计失败次数（未达锁定阈值时仍有值） */
    private int failCount;

    public static LoginLockStatusVO unlocked(int failCount) {
        return new LoginLockStatusVO(false, 0, Math.max(failCount, 0));
    }

    public static LoginLockStatusVO locked(long remainSeconds) {
        return new LoginLockStatusVO(true, Math.max(remainSeconds, 0), 0);
    }
}

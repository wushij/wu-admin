package cn.rbac.server.common.util;

import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import org.springframework.util.StringUtils;

/**
 * 用户展示名：优先昵称，无昵称时回退用户名。
 */
public final class UserDisplayNames {

    private UserDisplayNames() {
    }

    public static String of(UserDO user) {
        if (user == null) {
            return "-";
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return "-";
    }
}

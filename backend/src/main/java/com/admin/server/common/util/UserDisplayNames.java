package com.admin.server.common.util;

import org.springframework.util.StringUtils;

/**
 * 用户展示名：优先昵称，无昵称时回退用户名。
 */
public final class UserDisplayNames {

    private UserDisplayNames() {
    }

    public static String of(String nickname, String username) {
        if (StringUtils.hasText(nickname)) {
            return nickname.trim();
        }
        if (StringUtils.hasText(username)) {
            return username.trim();
        }
        return "-";
    }
}

package cn.rbac.server.modules.system.controller.admin.monitor.vo;

import lombok.Data;

/**
 * API 访问用户排行
 */
@Data
public class ApiAccessUserRankVO {

    private Long userId;

    /** 登录名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 访问次数 */
    private Long count;
}

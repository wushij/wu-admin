package cn.rbac.server.modules.system.dal.dataobject.user;

import lombok.Data;

/**
 * 岗位-用户数量聚合结果（避免加载全部 sys_user_post 到内存）
 */
@Data
public class PostUserCountVO {
    private Long postId;
    private Long userCount;
}

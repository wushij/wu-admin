package cn.rbac.server.modules.system.service.dashboard;

import cn.rbac.server.modules.system.dal.dataobject.loginlog.LoginLogDO;

import java.util.List;
import java.util.Map;

/**
 * 工作台统计 Service
 */
public interface DashboardService {

    /**
     * 聚合工作台统计数据
     *
     * @param loginUserId 当前登录用户 ID；非空时刷新在线活跃时间
     */
    Map<String, Object> getStats(Long loginUserId);

    /** 最近登录记录（最多 8 条） */
    List<LoginLogDO> getRecentLogins();

    /**
     * 记录工作台访问（Redis 日计数 +1）
     *
     * @param loginUserId 当前登录用户 ID；非空时刷新在线活跃时间
     */
    void recordVisit(Long loginUserId);
}

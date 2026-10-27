package com.admin.server.modules.system.service.security;

import com.admin.server.modules.system.dal.dataobject.security.SecurityEventDO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

/**
 * 安全事件审计日志服务
 */
public interface SecurityEventService {

    /**
     * 分页查询安全事件
     */
    Page<SecurityEventDO> getPage(Integer pageNo, Integer pageSize, String ipaddr, String eventType, String action);

    /**
     * 异步记录安全事件
     */
    void recordAsync(SecurityEventDO event);

    /**
     * 查询某个风险主体最近的安全事件。
     *
     * @param principal 主体标识，如 user:12 / ip:1.2.3.4
     * @param limit     返回条数上限
     */
    List<SecurityEventDO> getRecentByPrincipal(String principal, int limit);

    /**
     * 获取今日安全大盘统计数据
     */
    Map<String, Object> getDashboardStats();

    /**
     * 删除单条安全事件
     */
    void delete(Long id);

    /**
     * 清空所有安全事件
     */
    void clear();
}

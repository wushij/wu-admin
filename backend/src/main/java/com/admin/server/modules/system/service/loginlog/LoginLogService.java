package com.admin.server.modules.system.service.loginlog;

import com.admin.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 登录日志 Service 接口
 */
public interface LoginLogService {

    Page<LoginLogDO> getPage(Integer pageNo, Integer pageSize, String username, String ipaddr, Integer status);

    /**
     * 同步写入登录日志（成功登录用，避免工作台统计读到旧值）
     */
    void record(LoginLogDO log);

    /**
     * 异步写入登录日志（不阻塞登录接口）
     */
    void recordAsync(LoginLogDO log);

    void delete(Long id);

    void clear();
}

package cn.rbac.server.modules.system.service.loginlog;

import cn.rbac.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 登录日志 Service 接口
 */
public interface LoginLogService {

    Page<LoginLogDO> getPage(Integer pageNo, Integer pageSize, String username, String ipaddr, Integer status);

    /**
     * 异步写入登录日志（不阻塞登录接口）
     */
    void recordAsync(LoginLogDO log);

    void delete(Long id);

    void clear();
}

package cn.rbac.server.modules.system.service.loginlog;

import cn.rbac.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 登录日志 Service 接口
 */
public interface LoginLogService {

    /**
     * 获取登录日志分页
     */
    Page<LoginLogDO> getPage(Integer pageNo, Integer pageSize, String username, String ipaddr, Integer status);

    /**
     * 删除登录日志
     */
    void delete(Long id);

    /**
     * 清空登录日志
     */
    void clear();
}

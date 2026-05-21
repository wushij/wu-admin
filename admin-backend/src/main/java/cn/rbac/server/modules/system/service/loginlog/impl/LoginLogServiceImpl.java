package cn.rbac.server.modules.system.service.loginlog.impl;

import cn.rbac.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import cn.rbac.server.modules.system.dal.mysql.loginlog.LoginLogMapper;
import cn.rbac.server.modules.system.service.loginlog.LoginLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

/**
 * 登录日志 Service 实现类
 */
@Service
public class LoginLogServiceImpl implements LoginLogService {

    @Resource
    private LoginLogMapper loginLogMapper;

    @Override
    public Page<LoginLogDO> getPage(Integer pageNo, Integer pageSize, String username, String ipaddr, Integer status) {
        Page<LoginLogDO> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<LoginLogDO> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(username)) {
            wrapper.like(LoginLogDO::getUsername, username);
        }
        if (StringUtils.hasText(ipaddr)) {
            wrapper.like(LoginLogDO::getIpaddr, ipaddr);
        }
        if (status != null) {
            wrapper.eq(LoginLogDO::getStatus, status);
        }
        
        wrapper.orderByDesc(LoginLogDO::getLoginTime);
        
        return loginLogMapper.selectPage(page, wrapper);
    }

    @Override
    public void delete(Long id) {
        loginLogMapper.deleteById(id);
    }

    @Override
    public void clear() {
        loginLogMapper.delete(new LambdaQueryWrapper<>());
    }
}

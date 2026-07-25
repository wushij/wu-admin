package com.admin.server.modules.system.service.loginlog.impl;

import com.admin.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import com.admin.server.modules.system.dal.mysql.loginlog.LoginLogMapper;
import com.admin.server.modules.system.service.loginlog.LoginLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;

@Slf4j
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
    @Async
    public void recordAsync(LoginLogDO loginLog) {
        try {
            loginLogMapper.insert(loginLog);
        } catch (Exception e) {
            log.error("异步写入登录日志失败 username={}", loginLog != null ? loginLog.getUsername() : null, e);
        }
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

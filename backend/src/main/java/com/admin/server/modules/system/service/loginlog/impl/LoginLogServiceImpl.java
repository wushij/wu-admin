package com.admin.server.modules.system.service.loginlog.impl;

import com.admin.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import com.admin.server.modules.system.dal.mysql.loginlog.LoginLogMapper;
import com.admin.server.modules.system.service.loginlog.LoginLogService;
import com.admin.server.modules.system.service.permission.DataScopeService;
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
    @Resource
    private DataScopeService dataScopeService;

    @Override
    public Page<LoginLogDO> getPage(Integer pageNo, Integer pageSize, String username, String ipaddr, Integer status) {
        Page<LoginLogDO> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<LoginLogDO> wrapper = new LambdaQueryWrapper<>();
        // 组织隔离：仅展示数据范围内用户的登录日志
        dataScopeService.applyCreatorScope(wrapper, LoginLogDO::getUserId);

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
    public void record(LoginLogDO loginLog) {
        loginLogMapper.insert(loginLog);
    }

    @Override
    @Async
    public void recordAsync(LoginLogDO loginLog) {
        try {
            record(loginLog);
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

    @Override
    public long countByStartTime(java.time.LocalDateTime startTime, Integer status) {
        LambdaQueryWrapper<LoginLogDO> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) {
            wrapper.ge(LoginLogDO::getLoginTime, startTime);
        }
        if (status != null) {
            wrapper.eq(LoginLogDO::getStatus, status);
        }
        return loginLogMapper.selectCount(wrapper);
    }
}

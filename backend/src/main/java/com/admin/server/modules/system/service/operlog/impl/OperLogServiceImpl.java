package com.admin.server.modules.system.service.operlog.impl;

import com.admin.server.common.pojo.PageResult;
import com.admin.server.modules.system.dal.dataobject.operlog.OperLogDO;
import com.admin.server.modules.system.dal.mysql.operlog.OperLogMapper;
import com.admin.server.modules.system.service.operlog.OperLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperLogServiceImpl extends ServiceImpl<OperLogMapper, OperLogDO> implements OperLogService {

    @Override
    public PageResult<OperLogDO> page(Integer pageNo, Integer pageSize, String title, String operName, Integer status) {
        Page<OperLogDO> pageParam = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<OperLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(title), OperLogDO::getTitle, title)
                .like(StringUtils.hasText(operName), OperLogDO::getOperName, operName)
                .eq(status != null, OperLogDO::getStatus, status)
                .orderByDesc(OperLogDO::getOperTime);
        Page<OperLogDO> result = page(pageParam, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    @Override
    @Async
    public void recordLog(OperLogDO operLog) {
        save(operLog);
    }

    @Override
    public void delete(Long id) {
        removeById(id);
    }

    @Override
    public void clean() {
        remove(new LambdaQueryWrapper<>());
    }
}

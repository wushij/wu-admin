package com.admin.server.modules.system.service.operlog.impl;

import com.admin.server.common.pojo.PageResult;
import com.admin.server.common.util.UserDisplayNames;
import com.admin.server.modules.system.dal.dataobject.operlog.OperLogDO;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;
import com.admin.server.modules.system.dal.mysql.operlog.OperLogMapper;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;
import com.admin.server.modules.system.service.operlog.OperLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OperLogServiceImpl extends ServiceImpl<OperLogMapper, OperLogDO> implements OperLogService {

    @Resource
    private UserMapper userMapper;

    @Override
    public PageResult<OperLogDO> page(Integer pageNo, Integer pageSize, String title, String operName, Integer status) {
        Page<OperLogDO> pageParam = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<OperLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(title), OperLogDO::getTitle, title)
                .eq(status != null, OperLogDO::getStatus, status);

        if (StringUtils.hasText(operName)) {
            String search = operName.trim();
            List<UserDO> matchedUsers = userMapper.selectList(new LambdaQueryWrapper<UserDO>()
                    .eq(UserDO::getDeleted, 0)
                    .and(q -> q.like(UserDO::getUsername, search).or().like(UserDO::getNickname, search)));
            Set<String> searchKeys = new HashSet<>();
            searchKeys.add(search);
            for (UserDO u : matchedUsers) {
                if (StringUtils.hasText(u.getUsername())) {
                    searchKeys.add(u.getUsername());
                }
                if (StringUtils.hasText(u.getNickname())) {
                    searchKeys.add(u.getNickname());
                }
            }
            wrapper.and(w -> {
                int i = 0;
                for (String key : searchKeys) {
                    if (i++ > 0) {
                        w.or();
                    }
                    w.like(OperLogDO::getOperName, key);
                }
            });
        }

        wrapper.orderByDesc(OperLogDO::getOperTime);
        Page<OperLogDO> result = page(pageParam, wrapper);

        enrichOperName(result.getRecords());

        return PageResult.of(result.getRecords(), result.getTotal());
    }

    private void enrichOperName(List<OperLogDO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<String> operNames = records.stream()
                .map(OperLogDO::getOperName)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (operNames.isEmpty()) {
            return;
        }

        List<UserDO> users = userMapper.selectList(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getDeleted, 0)
                .and(q -> q.in(UserDO::getUsername, operNames).or().in(UserDO::getNickname, operNames)));

        Map<String, String> displayNameMap = new HashMap<>();
        for (UserDO u : users) {
            String displayName = UserDisplayNames.of(u);
            if (StringUtils.hasText(u.getUsername())) {
                displayNameMap.put(u.getUsername(), displayName);
            }
            if (StringUtils.hasText(u.getNickname())) {
                displayNameMap.put(u.getNickname(), displayName);
            }
        }

        for (OperLogDO log : records) {
            if (StringUtils.hasText(log.getOperName()) && displayNameMap.containsKey(log.getOperName())) {
                log.setOperName(displayNameMap.get(log.getOperName()));
            }
        }
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

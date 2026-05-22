package cn.rbac.server.modules.system.service.monitor.impl;

import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.api.monitor.vo.ApiAccessUserRankVO;
import cn.rbac.server.modules.system.dal.dataobject.monitor.ApiAccessLogDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.dal.mysql.monitor.ApiAccessLogMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.service.monitor.ApiAccessLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RDeque;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ApiAccessLogServiceImpl extends ServiceImpl<ApiAccessLogMapper, ApiAccessLogDO>
        implements ApiAccessLogService {

    private static final String REDIS_QUEUE_KEY = "api:access:log:queue";
    private static final int BATCH_SIZE = 500;

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private UserMapper userMapper;

    private RDeque<String> queue() {
        return redissonClient.getDeque(REDIS_QUEUE_KEY);
    }

    @Override
    @Async
    public void pushToRedis(ApiAccessLogDO accessLog) {
        try {
            String json = objectMapper.writeValueAsString(accessLog);
            queue().addFirst(json);
        } catch (Exception e) {
            log.error("API 访问日志写入 Redis 失败", e);
        }
    }

    @Override
    public void flushFromRedisToDb() {
        try {
            List<ApiAccessLogDO> batch = new ArrayList<>();
            RDeque<String> deque = queue();
            for (int i = 0; i < BATCH_SIZE; i++) {
                String json = deque.pollLast();
                if (json == null) {
                    break;
                }
                try {
                    batch.add(objectMapper.readValue(json, ApiAccessLogDO.class));
                } catch (Exception e) {
                    log.warn("解析 API 日志 JSON 失败: {}", json, e);
                }
            }
            if (!batch.isEmpty()) {
                saveBatch(batch);
                log.debug("API 访问日志批量落库: {} 条", batch.size());
            }
        } catch (Exception e) {
            log.error("API 访问日志批量落库失败", e);
        }
    }

    @Override
    public PageResult<ApiAccessLogDO> page(Integer pageNo, Integer pageSize, Long userId, String apiPath, String method,
                                           Integer success, LocalDateTime startTime, LocalDateTime endTime) {
        Page<ApiAccessLogDO> pageParam = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<ApiAccessLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, ApiAccessLogDO::getUserId, userId)
                .like(StringUtils.hasText(apiPath), ApiAccessLogDO::getApiPath, apiPath)
                .eq(StringUtils.hasText(method), ApiAccessLogDO::getMethod, method)
                .eq(success != null, ApiAccessLogDO::getSuccess, success)
                .ge(startTime != null, ApiAccessLogDO::getStartTime, startTime)
                .le(endTime != null, ApiAccessLogDO::getEndTime, endTime)
                .orderByDesc(ApiAccessLogDO::getStartTime);
        Page<ApiAccessLogDO> result = super.page(pageParam, wrapper);
        fillUsername(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    private void fillUsername(List<ApiAccessLogDO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> userIds = records.stream()
                .map(ApiAccessLogDO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UserDO> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (UserDO user : userMapper.selectBatchIds(userIds)) {
                if (user != null) {
                    userMap.put(user.getId(), user);
                }
            }
        }
        for (ApiAccessLogDO log : records) {
            if (log.getUserId() == null) {
                log.setUsername("-");
                continue;
            }
            UserDO user = userMap.get(log.getUserId());
            if (user == null) {
                log.setUsername("用户" + log.getUserId());
                continue;
            }
            log.setUsername(user.getUsername());
        }
    }

    @Override
    public Map<String, Object> getStatistics(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        LambdaQueryWrapper<ApiAccessLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(ApiAccessLogDO::getStartTime, start, end);

        List<ApiAccessLogDO> list = list(wrapper);
        long total = list.size();
        long successCount = list.stream()
                .filter(l -> l.getSuccess() != null && l.getSuccess() == 1)
                .count();
        long failCount = total - successCount;

        Map<String, Map<String, Long>> dailyStats = new LinkedHashMap<>();
        for (LocalDate day = startDate; !day.isAfter(endDate); day = day.plusDays(1)) {
            Map<String, Long> empty = new HashMap<>();
            empty.put("total", 0L);
            empty.put("success", 0L);
            empty.put("fail", 0L);
            dailyStats.put(day.toString(), empty);
        }
        for (ApiAccessLogDO item : list) {
            if (item.getStartTime() == null) {
                continue;
            }
            String dateKey = item.getStartTime().toLocalDate().toString();
            Map<String, Long> dayMap = dailyStats.get(dateKey);
            if (dayMap == null) {
                continue;
            }
            dayMap.put("total", dayMap.get("total") + 1);
            if (item.getSuccess() != null && item.getSuccess() == 1) {
                dayMap.put("success", dayMap.get("success") + 1);
            } else {
                dayMap.put("fail", dayMap.get("fail") + 1);
            }
        }

        Map<String, Long> pathCount = new HashMap<>();
        for (ApiAccessLogDO item : list) {
            String path = item.getApiPath() != null ? item.getApiPath() : "unknown";
            pathCount.merge(path, 1L, Long::sum);
        }
        List<Map<String, Object>> topPaths = pathCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("apiPath", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Long> methodCount = new HashMap<>();
        for (ApiAccessLogDO item : list) {
            String m = item.getMethod() != null ? item.getMethod() : "unknown";
            methodCount.merge(m, 1L, Long::sum);
        }

        Map<Long, Long> userCount = new HashMap<>();
        for (ApiAccessLogDO item : list) {
            Long uid = item.getUserId();
            if (uid != null) {
                userCount.merge(uid, 1L, Long::sum);
            }
        }
        List<ApiAccessUserRankVO> topUsers = buildTopUsers(userCount, 10);

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", total);
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("dailyStats", dailyStats);
        result.put("topPaths", topPaths);
        result.put("methodCount", methodCount);
        result.put("topUsers", topUsers);
        return result;
    }

    private List<ApiAccessUserRankVO> buildTopUsers(Map<Long, Long> userCount, int limit) {
        if (userCount.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map.Entry<Long, Long>> sorted = userCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .collect(Collectors.toList());

        Set<Long> userIds = sorted.stream().map(Map.Entry::getKey).collect(Collectors.toSet());
        Map<Long, UserDO> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (UserDO user : userMapper.selectBatchIds(userIds)) {
                if (user != null) {
                    userMap.put(user.getId(), user);
                }
            }
        }

        List<ApiAccessUserRankVO> result = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : sorted) {
            ApiAccessUserRankVO vo = new ApiAccessUserRankVO();
            vo.setUserId(entry.getKey());
            vo.setCount(entry.getValue());
            UserDO user = userMap.get(entry.getKey());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
            } else {
                vo.setUsername("用户" + entry.getKey());
                vo.setNickname("");
            }
            result.add(vo);
        }
        return result;
    }
}

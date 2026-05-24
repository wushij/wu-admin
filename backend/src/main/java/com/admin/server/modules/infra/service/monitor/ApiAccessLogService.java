package com.admin.server.modules.infra.service.monitor;

import com.admin.server.common.core.PageResult;
import com.admin.server.modules.infra.dal.dataobject.monitor.ApiAccessLogDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public interface ApiAccessLogService extends IService<ApiAccessLogDO> {

    void pushToRedis(ApiAccessLogDO log);

    void flushFromRedisToDb();

    PageResult<ApiAccessLogDO> page(Integer pageNo, Integer pageSize, Long userId, String apiPath, String method,
                                    Integer success, LocalDateTime startTime, LocalDateTime endTime);

    Map<String, Object> getStatistics(LocalDate startDate, LocalDate endDate);
}

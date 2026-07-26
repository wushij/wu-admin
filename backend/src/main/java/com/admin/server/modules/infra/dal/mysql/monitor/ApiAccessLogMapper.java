package com.admin.server.modules.infra.dal.mysql.monitor;

import com.admin.server.modules.infra.dal.dataobject.monitor.ApiAccessLogDO;
import com.admin.server.modules.infra.service.monitor.vo.ApiAccessDailyStatVO;
import com.admin.server.modules.infra.service.monitor.vo.ApiAccessMethodStatVO;
import com.admin.server.modules.infra.service.monitor.vo.ApiAccessPathStatVO;
import com.admin.server.modules.infra.service.monitor.vo.ApiAccessSummaryVO;
import com.admin.server.modules.infra.service.monitor.vo.ApiAccessUserStatVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ApiAccessLogMapper extends BaseMapper<ApiAccessLogDO> {

    @Select("""
            SELECT COUNT(*) AS totalCount,
                   COALESCE(SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END), 0) AS successCount
            FROM sys_api_access_log
            WHERE start_time >= #{start} AND start_time < #{end}
            """)
    ApiAccessSummaryVO selectSummary(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("""
            SELECT DATE_FORMAT(start_time, '%Y-%m-%d') AS statDate,
                   COUNT(*) AS total,
                   COALESCE(SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END), 0) AS successCount
            FROM sys_api_access_log
            WHERE start_time >= #{start} AND start_time < #{end}
            GROUP BY DATE_FORMAT(start_time, '%Y-%m-%d')
            ORDER BY statDate
            """)
    List<ApiAccessDailyStatVO> selectDailyStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("""
            SELECT IFNULL(api_path, 'unknown') AS apiPath, COUNT(*) AS count
            FROM sys_api_access_log
            WHERE start_time >= #{start} AND start_time < #{end}
            GROUP BY api_path
            ORDER BY count DESC
            LIMIT #{limit}
            """)
    List<ApiAccessPathStatVO> selectTopPaths(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end,
                                             @Param("limit") int limit);

    @Select("""
            SELECT IFNULL(method, 'unknown') AS method, COUNT(*) AS count
            FROM sys_api_access_log
            WHERE start_time >= #{start} AND start_time < #{end}
            GROUP BY method
            """)
    List<ApiAccessMethodStatVO> selectMethodCounts(@Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);

    @Select("""
            SELECT user_id AS userId, COUNT(*) AS count
            FROM sys_api_access_log
            WHERE start_time >= #{start} AND start_time < #{end} AND user_id IS NOT NULL
            GROUP BY user_id
            ORDER BY count DESC
            LIMIT #{limit}
            """)
    List<ApiAccessUserStatVO> selectTopUsers(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end,
                                             @Param("limit") int limit);
}

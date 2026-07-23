package com.admin.server.modules.system.dal.mysql.job;

import com.admin.server.modules.system.dal.dataobject.job.SysJobLogDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface SysJobLogMapper extends BaseMapper<SysJobLogDO> {

    @Select("SELECT DATE_FORMAT(start_time, '%Y-%m-%d') AS exec_date, " +
            "SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS success_count, " +
            "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS fail_count " +
            "FROM sys_job_log " +
            "WHERE deleted = 0 AND start_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
            "GROUP BY DATE_FORMAT(start_time, '%Y-%m-%d') " +
            "ORDER BY exec_date")
    List<Map<String, Object>> selectDailyStats();

    @Select({
            "<script>",
            "SELECT * FROM sys_job_log",
            "WHERE deleted = 1",
            "<if test='jobName != null and jobName != \"\"'>",
            "  AND job_name LIKE CONCAT('%', #{jobName}, '%')",
            "</if>",
            "<if test='jobGroup != null and jobGroup != \"\"'>",
            "  AND job_group = #{jobGroup}",
            "</if>",
            "ORDER BY update_time DESC",
            "</script>"
    })
    IPage<SysJobLogDO> selectDeletedPage(Page<SysJobLogDO> page,
                                         @Param("jobName") String jobName,
                                         @Param("jobGroup") String jobGroup);

    @Update("UPDATE sys_job_log SET deleted = 0, update_time = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Long id);

    @Delete("DELETE FROM sys_job_log WHERE id = #{id} AND deleted = 1")
    int deletePhysicalById(@Param("id") Long id);

    @Delete("DELETE FROM sys_job_log WHERE start_time < #{cutoff}")
    int deletePhysicalOlderThan(@Param("cutoff") LocalDateTime cutoff);
}

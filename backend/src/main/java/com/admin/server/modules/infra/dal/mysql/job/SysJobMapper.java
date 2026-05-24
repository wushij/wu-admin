package com.admin.server.modules.infra.dal.mysql.job;

import com.admin.server.modules.infra.dal.dataobject.job.SysJobDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysJobMapper extends BaseMapper<SysJobDO> {

    @Select({
            "<script>",
            "SELECT * FROM sys_job",
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
    IPage<SysJobDO> selectDeletedPage(Page<SysJobDO> page,
                                      @Param("jobName") String jobName,
                                      @Param("jobGroup") String jobGroup);

    @Update("UPDATE sys_job SET deleted = 0, update_time = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Long id);

    @Delete("DELETE FROM sys_job WHERE id = #{id} AND deleted = 1")
    int deletePhysicalById(@Param("id") Long id);
}

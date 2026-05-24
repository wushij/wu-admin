package com.admin.server.modules.infra.dal.mysql.file;

import com.admin.server.modules.infra.dal.dataobject.file.SysFileDO;
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

@Mapper
public interface SysFileMapper extends BaseMapper<SysFileDO> {

    @Select({
            "<script>",
            "SELECT * FROM sys_file",
            "WHERE deleted = 1",
            "<if test='originalName != null and originalName != \"\"'>",
            "  AND original_name LIKE CONCAT('%', #{originalName}, '%')",
            "</if>",
            "ORDER BY update_time DESC",
            "</script>"
    })
    IPage<SysFileDO> selectDeletedPage(Page<SysFileDO> page, @Param("originalName") String originalName);

    @Select("SELECT * FROM sys_file WHERE id = #{id} AND deleted = 1")
    SysFileDO selectDeletedById(@Param("id") Long id);

    @Update("UPDATE sys_file SET deleted = 0, update_time = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Long id);

    @Delete("DELETE FROM sys_file WHERE id = #{id} AND deleted = 1")
    int deletePhysicalById(@Param("id") Long id);

    @Select("SELECT id FROM sys_file WHERE deleted = 1 AND update_time < #{cutoff} LIMIT 500")
    List<Long> selectExpiredRecycleIds(@Param("cutoff") LocalDateTime cutoff);
}

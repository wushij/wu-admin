package com.admin.server.modules.ticket.dal.mysql.ticket;

import com.admin.server.modules.ticket.dal.dataobject.ticket.TicketDO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TicketMapper extends BaseMapper<TicketDO> {

    @Select({
            "<script>",
            "SELECT * FROM sys_ticket",
            "WHERE deleted = 1",
            "<if test='title != null and title != \"\"'>",
            "  AND title LIKE CONCAT('%', #{title}, '%')",
            "</if>",
            "<if test='status != null and status != \"\"'>",
            "  AND status = #{status}",
            "</if>",
            "<if test='priority != null and priority != \"\"'>",
            "  AND priority = #{priority}",
            "</if>",
            "ORDER BY update_time DESC",
            "</script>"
    })
    IPage<TicketDO> selectDeletedPage(Page<TicketDO> page,
                                      @Param("title") String title,
                                      @Param("status") String status,
                                      @Param("priority") String priority);

    @Update("UPDATE sys_ticket SET deleted = 0, update_time = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Long id);

    @Delete("DELETE FROM sys_ticket WHERE id = #{id} AND deleted = 1")
    int deletePhysicalById(@Param("id") Long id);

    @Select("SELECT id FROM sys_ticket WHERE deleted = 1 AND update_time < #{cutoff} LIMIT 500")
    List<Long> selectExpiredRecycleIds(@Param("cutoff") LocalDateTime cutoff);
}

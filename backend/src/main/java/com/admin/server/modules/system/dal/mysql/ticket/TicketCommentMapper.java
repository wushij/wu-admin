package com.admin.server.modules.system.dal.mysql.ticket;

import com.admin.server.modules.system.dal.dataobject.ticket.TicketCommentDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface TicketCommentMapper extends BaseMapper<TicketCommentDO> {
    @Update("UPDATE sys_ticket_comment SET deleted = 0, update_time = NOW() WHERE ticket_id = #{ticketId}")
    int restoreByTicketId(@Param("ticketId") Long ticketId);

    @Delete("DELETE FROM sys_ticket_comment WHERE ticket_id = #{ticketId}")
    int deletePhysicalByTicketId(@Param("ticketId") Long ticketId);
}

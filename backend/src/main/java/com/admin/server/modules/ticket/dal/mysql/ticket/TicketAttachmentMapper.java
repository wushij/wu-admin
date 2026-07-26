package com.admin.server.modules.ticket.dal.mysql.ticket;

import com.admin.server.modules.ticket.dal.dataobject.ticket.TicketAttachmentDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface TicketAttachmentMapper extends BaseMapper<TicketAttachmentDO> {
    @Update("UPDATE sys_ticket_attachment SET deleted = 0, update_time = NOW() WHERE ticket_id = #{ticketId}")
    int restoreByTicketId(@Param("ticketId") Long ticketId);

    @Delete("DELETE FROM sys_ticket_attachment WHERE ticket_id = #{ticketId}")
    int deletePhysicalByTicketId(@Param("ticketId") Long ticketId);
}

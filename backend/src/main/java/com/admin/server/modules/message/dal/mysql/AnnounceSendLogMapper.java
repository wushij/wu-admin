package com.admin.server.modules.message.dal.mysql;

import com.admin.server.modules.message.dal.dataobject.AnnounceSendLogDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnounceSendLogMapper extends BaseMapper<AnnounceSendLogDO> {

    @Delete("DELETE FROM sys_announce_send_log WHERE announce_id = #{announceId}")
    int deletePhysicalByAnnounceId(@Param("announceId") Long announceId);
}

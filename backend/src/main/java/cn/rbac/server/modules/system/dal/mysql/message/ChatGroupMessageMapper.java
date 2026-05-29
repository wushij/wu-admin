package cn.rbac.server.modules.system.dal.mysql.message;

import cn.rbac.server.modules.system.dal.dataobject.message.ChatGroupMessageDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface ChatGroupMessageMapper extends BaseMapper<ChatGroupMessageDO> {

    @Delete("DELETE FROM sys_chat_group_message WHERE send_time < #{cutoff}")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}

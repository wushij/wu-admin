package cn.rbac.server.modules.system.dal.mysql.message;

import cn.rbac.server.modules.system.dal.dataobject.message.ChatMessageDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessageDO> {

    @Select("SELECT COUNT(*) FROM sys_chat_message WHERE receiver_id = #{userId} AND is_read = 0")
    long countUnread(Long userId);
}

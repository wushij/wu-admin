package com.admin.server.modules.message.dal.mysql;

import com.admin.server.modules.message.dal.dataobject.ChatGroupLatestVO;
import com.admin.server.modules.message.dal.dataobject.ChatGroupMessageDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface ChatGroupMessageMapper extends BaseMapper<ChatGroupMessageDO> {

    @Delete("DELETE FROM sys_chat_group_message WHERE send_time < #{cutoff}")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);

    @Select("""
            <script>
            SELECT m.group_id AS groupId, m.content AS content, m.msg_type AS msgType, m.send_time AS sendTime
            FROM sys_chat_group_message m
            INNER JOIN (
                SELECT group_id, MAX(id) AS max_id
                FROM sys_chat_group_message
                WHERE group_id IN
                <foreach collection="groupIds" item="id" open="(" separator="," close=")">
                  #{id}
                </foreach>
                GROUP BY group_id
            ) t ON m.id = t.max_id
            </script>
            """)
    List<ChatGroupLatestVO> selectLatestByGroupIds(@Param("groupIds") Collection<Long> groupIds);
}

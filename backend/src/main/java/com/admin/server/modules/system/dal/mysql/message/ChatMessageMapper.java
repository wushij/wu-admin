package com.admin.server.modules.system.dal.mysql.message;

import com.admin.server.modules.system.dal.dataobject.message.ChatMessageDO;
import com.admin.server.modules.system.dal.dataobject.message.ChatPeerLatestVO;
import com.admin.server.modules.system.dal.dataobject.message.ChatPeerUnreadVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessageDO> {

    @Select("SELECT COUNT(*) FROM sys_chat_message WHERE receiver_id = #{userId} AND is_read = 0")
    long countUnread(Long userId);

    @Delete("DELETE FROM sys_chat_message WHERE send_time < #{cutoff}")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);

    @Select("""
            SELECT m.content AS content, m.msg_type AS msgType, m.send_time AS sendTime,
                   CASE WHEN m.sender_id = #{userId} THEN m.receiver_id ELSE m.sender_id END AS peerId
            FROM sys_chat_message m
            INNER JOIN (
                SELECT CASE WHEN sender_id = #{userId} THEN receiver_id ELSE sender_id END AS peer_id,
                       MAX(id) AS max_id
                FROM sys_chat_message
                WHERE sender_id = #{userId} OR receiver_id = #{userId}
                GROUP BY peer_id
            ) t ON m.id = t.max_id
            """)
    List<ChatPeerLatestVO> selectLatestPerPeer(@Param("userId") Long userId);

    @Select("""
            SELECT sender_id AS senderId, COUNT(*) AS unreadCount
            FROM sys_chat_message
            WHERE receiver_id = #{userId} AND is_read = 0
            GROUP BY sender_id
            """)
    List<ChatPeerUnreadVO> selectUnreadCountPerSender(@Param("userId") Long userId);
}

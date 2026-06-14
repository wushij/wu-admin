package cn.rbac.server.modules.system.dal.mysql.message;

import cn.rbac.server.modules.system.dal.dataobject.message.ChatGroupMemberCountVO;
import cn.rbac.server.modules.system.dal.dataobject.message.ChatGroupMemberDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ChatGroupMemberMapper extends BaseMapper<ChatGroupMemberDO> {

    @Select("""
            <script>
            SELECT group_id AS groupId, COUNT(*) AS memberCount
            FROM sys_chat_group_member
            WHERE group_id IN
            <foreach collection="groupIds" item="id" open="(" separator="," close=")">
              #{id}
            </foreach>
            GROUP BY group_id
            </script>
            """)
    List<ChatGroupMemberCountVO> selectMemberCountByGroupIds(@Param("groupIds") Collection<Long> groupIds);

    /**
     * 批量新增群成员。仅写入建群/邀请时设置的字段（group_id、user_id、role、join_time），
     * 其余字段（nickname、muted 等）由数据库默认值兜底，与逐条 insert 不显式 set 时的行为一致。
     */
    @Insert("""
            <script>
            INSERT INTO sys_chat_group_member (group_id, user_id, role, join_time) VALUES
            <foreach collection="list" item="m" separator=",">
              (#{m.groupId}, #{m.userId}, #{m.role}, #{m.joinTime})
            </foreach>
            </script>
            """)
    int insertBatch(@Param("list") List<ChatGroupMemberDO> members);
}

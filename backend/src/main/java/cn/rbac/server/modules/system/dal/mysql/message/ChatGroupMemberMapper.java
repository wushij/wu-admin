package cn.rbac.server.modules.system.dal.mysql.message;

import cn.rbac.server.modules.system.dal.dataobject.message.ChatGroupMemberCountVO;
import cn.rbac.server.modules.system.dal.dataobject.message.ChatGroupMemberDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
}

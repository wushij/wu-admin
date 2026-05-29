package cn.rbac.server.modules.system.dal.mysql.message;

import cn.rbac.server.modules.system.dal.dataobject.message.UserAnnounceDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserAnnounceMapper extends BaseMapper<UserAnnounceDO> {

    @Select("SELECT COUNT(*) FROM sys_user_announce WHERE user_id = #{userId} AND is_read = 0")
    long countUnread(Long userId);
}

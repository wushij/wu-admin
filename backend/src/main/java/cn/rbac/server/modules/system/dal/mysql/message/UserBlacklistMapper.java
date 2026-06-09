package cn.rbac.server.modules.system.dal.mysql.message;

import cn.rbac.server.modules.system.dal.dataobject.message.UserBlacklistDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserBlacklistMapper extends BaseMapper<UserBlacklistDO> {

    @Select("SELECT blocked_user_id FROM sys_user_blacklist WHERE user_id = #{userId}")
    List<Long> selectBlockedUserIds(@Param("userId") Long userId);
}

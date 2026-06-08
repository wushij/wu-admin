package cn.rbac.server.modules.system.dal.mysql.permission;

import cn.rbac.server.modules.system.dal.dataobject.permission.UserRoleDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.Collection;
import java.util.List;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleDO> {

    @Select("SELECT * FROM sys_user_role WHERE user_id = #{userId}")
    List<UserRoleDO> selectListByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);

    @Select("<script>SELECT * FROM sys_user_role WHERE user_id IN <foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<UserRoleDO> selectByUserIds(@Param("userIds") Collection<Long> userIds);
}

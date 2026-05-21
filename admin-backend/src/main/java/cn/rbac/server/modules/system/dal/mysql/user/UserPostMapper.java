package cn.rbac.server.modules.system.dal.mysql.user;

import cn.rbac.server.modules.system.dal.dataobject.user.UserPostDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserPostMapper extends BaseMapper<UserPostDO> {

    @Delete("DELETE FROM sys_user_post WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);

    @Select("SELECT user_id FROM sys_user_post WHERE post_id = #{postId}")
    List<Long> selectUserIdsByPostId(@Param("postId") Long postId);

    @Select("SELECT post_id FROM sys_user_post WHERE user_id = #{userId}")
    List<Long> selectPostIdsByUserId(@Param("userId") Long userId);
}

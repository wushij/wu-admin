package com.admin.server.modules.system.dal.mysql.user;

import com.admin.server.modules.system.dal.dataobject.user.PostUserCountVO;
import com.admin.server.modules.system.dal.dataobject.user.UserPostDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface UserPostMapper extends BaseMapper<UserPostDO> {

    @Delete("DELETE FROM sys_user_post WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);

    @Select("SELECT user_id FROM sys_user_post WHERE post_id = #{postId}")
    List<Long> selectUserIdsByPostId(@Param("postId") Long postId);

    @Select("SELECT post_id FROM sys_user_post WHERE user_id = #{userId}")
    List<Long> selectPostIdsByUserId(@Param("userId") Long userId);

    @Select("<script>SELECT * FROM sys_user_post WHERE user_id IN <foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<UserPostDO> selectByUserIds(@Param("userIds") Collection<Long> userIds);

    @Select("""
            <script>
            SELECT post_id AS postId, COUNT(*) AS userCount
            FROM sys_user_post
            WHERE post_id IN
            <foreach collection="postIds" item="id" open="(" separator="," close=")">
              #{id}
            </foreach>
            GROUP BY post_id
            </script>
            """)
    List<PostUserCountVO> selectUserCountByPostIds(@Param("postIds") Collection<Long> postIds);
}


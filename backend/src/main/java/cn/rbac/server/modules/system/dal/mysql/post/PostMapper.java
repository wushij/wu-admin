package cn.rbac.server.modules.system.dal.mysql.post;

import cn.rbac.server.modules.system.dal.dataobject.post.PostDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PostMapper extends BaseMapper<PostDO> {

    @Select({
            "<script>",
            "SELECT * FROM sys_post",
            "WHERE deleted = 1",
            "<if test='postName != null and postName != \"\"'>",
            "  AND post_name LIKE CONCAT('%', #{postName}, '%')",
            "</if>",
            "<if test='status != null'>",
            "  AND status = #{status}",
            "</if>",
            "ORDER BY update_time DESC",
            "</script>"
    })
    IPage<PostDO> selectDeletedPage(Page<PostDO> page,
                                    @Param("postName") String postName,
                                    @Param("status") Integer status);

    @Update("UPDATE sys_post SET deleted = 0, update_time = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Long id);

    @Delete("DELETE FROM sys_post WHERE id = #{id} AND deleted = 1")
    int deletePhysicalById(@Param("id") Long id);
}

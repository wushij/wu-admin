package cn.rbac.server.modules.system.dal.mysql.message;

import cn.rbac.server.modules.system.dal.dataobject.message.AnnounceDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AnnounceMapper extends BaseMapper<AnnounceDO> {

    @Select({
            "<script>",
            "SELECT * FROM sys_announce",
            "WHERE deleted = 1",
            "<if test='title != null and title != \"\"'>",
            "  AND title LIKE CONCAT('%', #{title}, '%')",
            "</if>",
            "ORDER BY update_time DESC",
            "</script>"
    })
    IPage<AnnounceDO> selectDeletedPage(Page<AnnounceDO> page, @Param("title") String title);

    @Update("UPDATE sys_announce SET deleted = 0, update_time = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Long id);

    @Delete("DELETE FROM sys_announce WHERE id = #{id} AND deleted = 1")
    int deletePhysicalById(@Param("id") Long id);
}

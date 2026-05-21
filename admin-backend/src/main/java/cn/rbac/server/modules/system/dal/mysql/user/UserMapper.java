package cn.rbac.server.modules.system.dal.mysql.user;

import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface UserMapper extends BaseMapper<UserDO> {
    @Select({
            "<script>",
            "SELECT * FROM sys_user",
            "WHERE deleted = 1",
            "<if test='username != null and username != \"\"'>",
            "  AND username LIKE CONCAT('%', #{username}, '%')",
            "</if>",
            "<if test='mobile != null and mobile != \"\"'>",
            "  AND mobile LIKE CONCAT('%', #{mobile}, '%')",
            "</if>",
            "<if test='status != null'>",
            "  AND status = #{status}",
            "</if>",
            "<if test='deptId != null'>",
            "  AND dept_id = #{deptId}",
            "</if>",
            "ORDER BY update_time DESC",
            "</script>"
    })
    IPage<UserDO> selectDeletedPage(Page<UserDO> page,
                                    @Param("username") String username,
                                    @Param("mobile") String mobile,
                                    @Param("status") Integer status,
                                    @Param("deptId") Long deptId);

    @Update("UPDATE sys_user SET deleted = 0, update_time = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Long id);

    @Delete("DELETE FROM sys_user WHERE id = #{id} AND deleted = 1")
    int deletePhysicalById(@Param("id") Long id);
}

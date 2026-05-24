package com.admin.server.modules.system.dal.mysql.dept;

import com.admin.server.modules.system.dal.dataobject.dept.DeptDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface DeptMapper extends BaseMapper<DeptDO> {
    @Select({
            "<script>",
            "SELECT * FROM sys_dept",
            "WHERE deleted = 1",
            "<if test='name != null and name != \"\"'>",
            "  AND name LIKE CONCAT('%', #{name}, '%')",
            "</if>",
            "<if test='status != null'>",
            "  AND status = #{status}",
            "</if>",
            "ORDER BY update_time DESC",
            "</script>"
    })
    IPage<DeptDO> selectDeletedPage(Page<DeptDO> page,
                                    @Param("name") String name,
                                    @Param("status") Integer status);

    @Update("UPDATE sys_dept SET deleted = 0, update_time = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Long id);

    @Delete("DELETE FROM sys_dept WHERE id = #{id} AND deleted = 1")
    int deletePhysicalById(@Param("id") Long id);
}

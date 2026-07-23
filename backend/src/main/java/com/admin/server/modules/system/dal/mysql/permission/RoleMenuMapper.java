package com.admin.server.modules.system.dal.mysql.permission;

import com.admin.server.modules.system.dal.dataobject.permission.RoleMenuDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenuDO> {

    @Select("SELECT * FROM sys_role_menu WHERE role_id = #{roleId}")
    List<RoleMenuDO> selectListByRoleId(@Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    void deleteByRoleId(@Param("roleId") Long roleId);

    /** 批量新增角色-菜单关联（角色授权场景） */
    @Insert("""
            <script>
            INSERT INTO sys_role_menu (role_id, menu_id) VALUES
            <foreach collection="list" item="rm" separator=",">
              (#{rm.roleId}, #{rm.menuId})
            </foreach>
            </script>
            """)
    int insertBatch(@Param("list") List<RoleMenuDO> records);
}

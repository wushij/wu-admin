package cn.rbac.server.modules.system.dal.mysql.dict;

import cn.rbac.server.modules.system.dal.dataobject.dict.DictTypeDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DictTypeMapper extends BaseMapper<DictTypeDO> {

    @Select({
            "<script>",
            "SELECT * FROM sys_dict_type",
            "WHERE deleted = 1",
            "<if test='dictName != null and dictName != \"\"'>",
            "  AND dict_name LIKE CONCAT('%', #{dictName}, '%')",
            "</if>",
            "<if test='dictType != null and dictType != \"\"'>",
            "  AND dict_type LIKE CONCAT('%', #{dictType}, '%')",
            "</if>",
            "ORDER BY update_time DESC",
            "</script>"
    })
    IPage<DictTypeDO> selectDeletedPage(Page<DictTypeDO> page,
                                        @Param("dictName") String dictName,
                                        @Param("dictType") String dictType);

    @Update("UPDATE sys_dict_type SET deleted = 0, update_time = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Long id);

    @Delete("DELETE FROM sys_dict_type WHERE id = #{id} AND deleted = 1")
    int deletePhysicalById(@Param("id") Long id);
}

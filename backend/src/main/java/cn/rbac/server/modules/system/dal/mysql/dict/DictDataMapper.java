package cn.rbac.server.modules.system.dal.mysql.dict;

import cn.rbac.server.modules.system.dal.dataobject.dict.DictDataDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DictDataMapper extends BaseMapper<DictDataDO> {

    @Select("SELECT * FROM sys_dict_data WHERE dict_type = #{dictType} AND status = 1 AND deleted = 0 ORDER BY sort ASC, id ASC")
    List<DictDataDO> selectEnabledByDictType(@Param("dictType") String dictType);

    @Select({
            "<script>",
            "SELECT * FROM sys_dict_data",
            "WHERE deleted = 1",
            "<if test='dictType != null and dictType != \"\"'>",
            "  AND dict_type LIKE CONCAT('%', #{dictType}, '%')",
            "</if>",
            "<if test='dictLabel != null and dictLabel != \"\"'>",
            "  AND dict_label LIKE CONCAT('%', #{dictLabel}, '%')",
            "</if>",
            "ORDER BY update_time DESC",
            "</script>"
    })
    IPage<DictDataDO> selectDeletedPage(Page<DictDataDO> page,
                                        @Param("dictType") String dictType,
                                        @Param("dictLabel") String dictLabel);

    @Update("UPDATE sys_dict_data SET deleted = 0, update_time = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Long id);

    @Delete("DELETE FROM sys_dict_data WHERE id = #{id} AND deleted = 1")
    int deletePhysicalById(@Param("id") Long id);
}

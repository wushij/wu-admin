package cn.rbac.server.modules.system.dal.mysql.dict;

import cn.rbac.server.modules.system.dal.dataobject.dict.DictDataDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DictDataMapper extends BaseMapper<DictDataDO> {

    @Select("SELECT * FROM sys_dict_data WHERE dict_type = #{dictType} AND status = 1 AND deleted = 0 ORDER BY sort ASC, id ASC")
    List<DictDataDO> selectEnabledByDictType(@Param("dictType") String dictType);
}

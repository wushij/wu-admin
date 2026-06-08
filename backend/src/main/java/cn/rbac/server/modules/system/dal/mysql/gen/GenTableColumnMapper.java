package cn.rbac.server.modules.system.dal.mysql.gen;

import cn.rbac.server.modules.system.dal.dataobject.gen.GenTableColumnDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GenTableColumnMapper extends BaseMapper<GenTableColumnDO> {

    default List<GenTableColumnDO> selectByTableId(Long tableId) {
        return selectList(new LambdaQueryWrapper<GenTableColumnDO>()
                .eq(GenTableColumnDO::getTableId, tableId)
                .orderByAsc(GenTableColumnDO::getSort));
    }

    default int deleteByTableId(Long tableId) {
        return delete(new LambdaQueryWrapper<GenTableColumnDO>()
                .eq(GenTableColumnDO::getTableId, tableId));
    }
}

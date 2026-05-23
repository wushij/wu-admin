package cn.rbac.server.modules.system.dal.mysql.dict;

import cn.rbac.server.modules.system.dal.dataobject.dict.DictTypeDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DictTypeMapper extends BaseMapper<DictTypeDO> {
}

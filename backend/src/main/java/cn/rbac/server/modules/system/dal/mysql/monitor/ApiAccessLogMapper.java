package cn.rbac.server.modules.system.dal.mysql.monitor;

import cn.rbac.server.modules.system.dal.dataobject.monitor.ApiAccessLogDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApiAccessLogMapper extends BaseMapper<ApiAccessLogDO> {
}

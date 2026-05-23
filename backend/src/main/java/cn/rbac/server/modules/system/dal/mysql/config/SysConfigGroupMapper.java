package cn.rbac.server.modules.system.dal.mysql.config;

import cn.rbac.server.modules.system.dal.dataobject.config.SysConfigGroupDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysConfigGroupMapper extends BaseMapper<SysConfigGroupDO> {
}

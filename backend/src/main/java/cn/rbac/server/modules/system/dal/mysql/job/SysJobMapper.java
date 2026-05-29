package cn.rbac.server.modules.system.dal.mysql.job;

import cn.rbac.server.modules.system.dal.dataobject.job.SysJobDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysJobMapper extends BaseMapper<SysJobDO> {
}

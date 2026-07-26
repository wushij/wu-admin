package com.admin.server.modules.infra.dal.mysql.operlog;

import com.admin.server.modules.infra.dal.dataobject.operlog.OperLogDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperLogMapper extends BaseMapper<OperLogDO> {
}

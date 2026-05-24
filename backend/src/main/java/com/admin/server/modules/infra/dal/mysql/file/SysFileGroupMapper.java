package com.admin.server.modules.infra.dal.mysql.file;

import com.admin.server.modules.infra.dal.dataobject.file.SysFileGroupDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysFileGroupMapper extends BaseMapper<SysFileGroupDO> {
}

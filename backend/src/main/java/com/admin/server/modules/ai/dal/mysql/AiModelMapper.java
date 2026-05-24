package com.admin.server.modules.ai.dal.mysql;

import com.admin.server.modules.ai.dal.dataobject.AiModelDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiModelMapper extends BaseMapper<AiModelDO> {
}

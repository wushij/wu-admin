package com.admin.server.modules.ai.dal.mysql;

import com.admin.server.modules.ai.dal.dataobject.AiChatLogDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiChatLogMapper extends BaseMapper<AiChatLogDO> {
}

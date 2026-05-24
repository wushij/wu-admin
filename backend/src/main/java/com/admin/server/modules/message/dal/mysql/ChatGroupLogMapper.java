package com.admin.server.modules.message.dal.mysql;

import com.admin.server.modules.message.dal.dataobject.ChatGroupLogDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatGroupLogMapper extends BaseMapper<ChatGroupLogDO> {
}

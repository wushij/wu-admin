package com.admin.server.modules.message.dal.mysql;

import com.admin.server.modules.message.dal.dataobject.ChatGroupDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatGroupMapper extends BaseMapper<ChatGroupDO> {
}

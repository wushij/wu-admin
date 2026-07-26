package com.admin.server.modules.trade.dal.mysql.sms;

import com.admin.server.modules.trade.dal.dataobject.sms.SmsLogDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SmsLogMapper extends BaseMapper<SmsLogDO> {
}

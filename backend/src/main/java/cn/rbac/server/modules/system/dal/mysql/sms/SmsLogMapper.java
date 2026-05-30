package cn.rbac.server.modules.system.dal.mysql.sms;

import cn.rbac.server.modules.system.dal.dataobject.sms.SmsLogDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SmsLogMapper extends BaseMapper<SmsLogDO> {
}

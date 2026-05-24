package cn.rbac.server.modules.system.dal.mysql.message;

import cn.rbac.server.modules.system.dal.dataobject.message.AnnounceSendLogDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnnounceSendLogMapper extends BaseMapper<AnnounceSendLogDO> {
}

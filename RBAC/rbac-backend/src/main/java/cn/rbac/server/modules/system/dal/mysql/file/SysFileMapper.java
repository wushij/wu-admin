package cn.rbac.server.modules.system.dal.mysql.file;

import cn.rbac.server.modules.system.dal.dataobject.file.SysFileDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysFileMapper extends BaseMapper<SysFileDO> {
}

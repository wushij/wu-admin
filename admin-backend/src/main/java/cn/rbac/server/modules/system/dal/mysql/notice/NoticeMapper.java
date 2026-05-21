package cn.rbac.server.modules.system.dal.mysql.notice;

import cn.rbac.server.modules.system.dal.dataobject.notice.NoticeDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoticeMapper extends BaseMapper<NoticeDO> {
}

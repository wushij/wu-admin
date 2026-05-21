package cn.rbac.server.modules.system.dal.mysql.post;

import cn.rbac.server.modules.system.dal.dataobject.post.PostDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper extends BaseMapper<PostDO> {
}

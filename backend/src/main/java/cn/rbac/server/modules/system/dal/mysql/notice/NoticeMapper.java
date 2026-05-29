package cn.rbac.server.modules.system.dal.mysql.notice;

import cn.rbac.server.modules.system.dal.dataobject.notice.NoticeDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface NoticeMapper extends BaseMapper<NoticeDO> {

    @Delete("DELETE FROM sys_notice WHERE read_status = 1 AND create_time < #{cutoff}")
    int deleteReadOlderThan(@Param("cutoff") LocalDateTime cutoff);
}

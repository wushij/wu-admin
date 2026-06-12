package cn.rbac.server.modules.system.dal.mysql.notice;

import cn.rbac.server.modules.system.dal.dataobject.notice.NoticeDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface NoticeMapper extends BaseMapper<NoticeDO> {

    @Delete("DELETE FROM sys_notice WHERE read_status = 1 AND create_time < #{cutoff}")
    int deleteReadOlderThan(@Param("cutoff") LocalDateTime cutoff);

    @Delete("DELETE FROM sys_notice WHERE biz_type = #{bizType} AND biz_id = #{bizId}")
    int deletePhysicalByBiz(@Param("bizType") String bizType, @Param("bizId") Long bizId);

    @Update("UPDATE sys_notice SET read_status = 1 WHERE user_id = #{userId} AND read_status = 0")
    int markAllReadByUserId(@Param("userId") Long userId);
}

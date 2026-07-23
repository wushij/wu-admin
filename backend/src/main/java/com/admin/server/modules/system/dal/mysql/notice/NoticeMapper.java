package com.admin.server.modules.system.dal.mysql.notice;

import com.admin.server.modules.system.dal.dataobject.notice.NoticeDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NoticeMapper extends BaseMapper<NoticeDO> {

    /** 批量新增业务通知（工单全员通知等） */
    @Insert("""
            <script>
            INSERT INTO sys_notice (user_id, title, content, biz_type, biz_id, read_status, create_time, update_time, deleted) VALUES
            <foreach collection="list" item="n" separator=",">
              (#{n.userId}, #{n.title}, #{n.content}, #{n.bizType}, #{n.bizId}, #{n.readStatus}, #{n.createTime}, #{n.updateTime}, 0)
            </foreach>
            </script>
            """)
    int insertBatch(@Param("list") List<NoticeDO> records);

    @Delete("DELETE FROM sys_notice WHERE read_status = 1 AND create_time < #{cutoff}")
    int deleteReadOlderThan(@Param("cutoff") LocalDateTime cutoff);

    @Delete("DELETE FROM sys_notice WHERE biz_type = #{bizType} AND biz_id = #{bizId}")
    int deletePhysicalByBiz(@Param("bizType") String bizType, @Param("bizId") Long bizId);

    @Update("UPDATE sys_notice SET read_status = 1 WHERE user_id = #{userId} AND read_status = 0")
    int markAllReadByUserId(@Param("userId") Long userId);

    @Update("UPDATE sys_notice SET read_status = 1 WHERE user_id = #{userId} AND biz_type = #{bizType} AND biz_id = #{bizId} AND read_status = 0")
    int markReadByBiz(@Param("userId") Long userId, @Param("bizType") String bizType, @Param("bizId") Long bizId);
}

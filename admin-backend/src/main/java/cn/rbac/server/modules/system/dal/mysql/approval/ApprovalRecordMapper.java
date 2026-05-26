package cn.rbac.server.modules.system.dal.mysql.approval;

import cn.rbac.server.modules.system.dal.dataobject.approval.ApprovalRecordDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface ApprovalRecordMapper extends BaseMapper<ApprovalRecordDO> {
    @Update("UPDATE sys_approval_record SET deleted = 0, update_time = NOW() WHERE form_id = #{formId}")
    int restoreByFormId(@Param("formId") Long formId);

    @Delete("DELETE FROM sys_approval_record WHERE form_id = #{formId}")
    int deletePhysicalByFormId(@Param("formId") Long formId);
}

package com.admin.server.modules.ticket.dal.mysql.approval;

import com.admin.server.modules.ticket.dal.dataobject.approval.ApprovalFormDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface ApprovalFormMapper extends BaseMapper<ApprovalFormDO> {
    @Select({
            "<script>",
            "SELECT * FROM sys_approval_form",
            "WHERE deleted = 1",
            "<if test='title != null and title != \"\"'>",
            "  AND title LIKE CONCAT('%', #{title}, '%')",
            "</if>",
            "<if test='formType != null and formType != \"\"'>",
            "  AND form_type = #{formType}",
            "</if>",
            "<if test='status != null and status != \"\"'>",
            "  AND status = #{status}",
            "</if>",
            "ORDER BY update_time DESC",
            "</script>"
    })
    IPage<ApprovalFormDO> selectDeletedPage(Page<ApprovalFormDO> page,
                                            @Param("title") String title,
                                            @Param("formType") String formType,
                                            @Param("status") String status);

    @Update("UPDATE sys_approval_form SET deleted = 0, update_time = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Long id);

    @Delete("DELETE FROM sys_approval_form WHERE id = #{id} AND deleted = 1")
    int deletePhysicalById(@Param("id") Long id);
}

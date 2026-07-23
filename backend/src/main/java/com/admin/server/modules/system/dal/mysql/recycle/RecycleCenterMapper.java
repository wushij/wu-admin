package com.admin.server.modules.system.dal.mysql.recycle;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RecycleCenterMapper {

    @Select("SELECT COUNT(*) FROM sys_user WHERE deleted = 1")
    long countDeletedUsers();

    @Select("SELECT COUNT(*) FROM sys_role WHERE deleted = 1")
    long countDeletedRoles();

    @Select("SELECT COUNT(*) FROM sys_menu WHERE deleted = 1")
    long countDeletedMenus();

    @Select("SELECT COUNT(*) FROM sys_dept WHERE deleted = 1")
    long countDeletedDepts();

    @Select("SELECT COUNT(*) FROM sys_post WHERE deleted = 1")
    long countDeletedPosts();

    @Select("SELECT COUNT(*) FROM sys_ticket WHERE deleted = 1")
    long countDeletedTickets();

    @Select("SELECT COUNT(*) FROM sys_approval_form WHERE deleted = 1")
    long countDeletedApprovals();

    @Select("SELECT COUNT(*) FROM sys_dict_type WHERE deleted = 1")
    long countDeletedDictTypes();

    @Select("SELECT COUNT(*) FROM sys_dict_data WHERE deleted = 1")
    long countDeletedDictData();

    @Select("SELECT COUNT(*) FROM sys_announce WHERE deleted = 1")
    long countDeletedAnnounces();

    @Select("SELECT COUNT(*) FROM sys_job WHERE deleted = 1")
    long countDeletedJobs();

    @Select("SELECT COUNT(*) FROM sys_job_log WHERE deleted = 1")
    long countDeletedJobLogs();

    @Select("SELECT COUNT(*) FROM sys_file WHERE deleted = 1")
    long countDeletedFiles();

    @Select("SELECT COUNT(*) FROM gen_table WHERE deleted = 1")
    long countDeletedGenTables();
}

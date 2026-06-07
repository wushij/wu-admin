package cn.rbac.server.modules.system.dal.mysql.dashboard;

import cn.rbac.server.modules.system.service.dashboard.vo.DashboardStatsRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface DashboardMapper {

    /**
     * 单次查询聚合工作台计数类指标（替代多次 {@code selectCount} 往返）
     */
    @Select({
            "SELECT",
            "  (SELECT COUNT(*) FROM sys_user WHERE deleted = 0) AS user_count,",
            "  (SELECT COUNT(*) FROM sys_user WHERE deleted = 0 AND status = 2) AS user_pending_count,",
            "  (SELECT COUNT(*) FROM sys_user WHERE deleted = 0 AND status = 0) AS user_disabled_count,",
            "  (SELECT COUNT(*) FROM sys_user WHERE deleted = 0 AND create_time >= #{todayStart}) AS user_today,",
            "  (SELECT COUNT(*) FROM sys_user WHERE deleted = 0 AND create_time >= #{yesterdayStart} AND create_time < #{todayStart}) AS user_yesterday,",
            "  (SELECT COUNT(*) FROM sys_role WHERE deleted = 0) AS role_count,",
            "  (SELECT COUNT(*) FROM sys_role WHERE deleted = 0 AND create_time >= #{todayStart}) AS role_today,",
            "  (SELECT COUNT(*) FROM sys_role WHERE deleted = 0 AND create_time >= #{yesterdayStart} AND create_time < #{todayStart}) AS role_yesterday,",
            "  (SELECT COUNT(*) FROM sys_menu WHERE deleted = 0) AS menu_count,",
            "  (SELECT COUNT(*) FROM sys_dept WHERE deleted = 0) AS dept_count,",
            "  (SELECT COUNT(*) FROM sys_dept WHERE deleted = 0 AND create_time >= #{todayStart}) AS dept_today,",
            "  (SELECT COUNT(*) FROM sys_dept WHERE deleted = 0 AND create_time >= #{yesterdayStart} AND create_time < #{todayStart}) AS dept_yesterday,",
            "  (SELECT COUNT(*) FROM sys_post WHERE deleted = 0) AS post_count,",
            "  (SELECT COUNT(*) FROM sys_login_log WHERE status = 0 AND login_time >= #{todayStart}) AS today_login_success,",
            "  (SELECT COUNT(*) FROM sys_login_log WHERE status = 1 AND login_time >= #{todayStart}) AS today_login_fail,",
            "  (SELECT COUNT(*) FROM sys_login_log WHERE status = 0 AND login_time >= #{yesterdayStart} AND login_time < #{todayStart}) AS yesterday_login_success,",
            "  (SELECT COUNT(*) FROM sys_file WHERE file_path NOT LIKE CONCAT(#{chatImagePrefix}, '%')",
            "    AND file_path NOT LIKE CONCAT(#{chatFilePrefix}, '%')) AS file_count,",
            "  (SELECT COUNT(*) FROM sys_ticket WHERE deleted = 0 AND status = 'OPEN') AS ticket_open_count,",
            "  (SELECT COUNT(*) FROM sys_ticket WHERE deleted = 0 AND status IN ('OPEN','IN_PROGRESS') AND deadline IS NOT NULL AND deadline < #{now}) AS ticket_overdue_count,",
            "  (SELECT COUNT(*) FROM sys_approval_form WHERE deleted = 0 AND status = 'SUBMITTED') AS approval_pending_count"
    })
    DashboardStatsRow selectAggregateStats(@Param("todayStart") LocalDateTime todayStart,
                                           @Param("yesterdayStart") LocalDateTime yesterdayStart,
                                           @Param("now") LocalDateTime now,
                                           @Param("chatImagePrefix") String chatImagePrefix,
                                           @Param("chatFilePrefix") String chatFilePrefix);
}

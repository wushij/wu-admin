-- =============================================================================
-- add6_wuadmin.sql  生产/服务器增量补丁 #6（可重复执行）
-- =============================================================================
-- 侧栏「业务中心」更名为「流程中心」，路径 /business → /workflow，图标 Operation；
-- 一级排序调至消息中心与开发工具之间（sort 7，开发工具 sort 8）。
-- 库名 wuadmin，与 application-prod.yml 一致。
-- 组织部门「业务中心」(sys_dept) 不变，仅改菜单 sys_menu id=8 / id=150。
--
-- 用法: mysql -u wuadmin -p wuadmin < sql/add6_wuadmin.sql
-- 执行后请重新登录以刷新侧栏菜单。
-- =============================================================================

USE `wuadmin`;

UPDATE sys_menu
SET name = '流程中心',
    path = '/workflow',
    icon = 'Operation',
    sort = 7
WHERE id = 8
  AND deleted = 0;

UPDATE sys_menu
SET sort = 8
WHERE id = 150
  AND deleted = 0;

SELECT '[OK] add6_wuadmin.sql finished' AS result;

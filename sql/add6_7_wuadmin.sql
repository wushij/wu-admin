-- =============================================================================
-- add6_7_wuadmin.sql  生产/服务器增量补丁 #6 + #7 + #8 + #9（可重复执行）
-- =============================================================================
-- #6 侧栏「业务中心」更名为「流程中心」，路径 /business → /workflow，图标 Operation；
--    一级排序调至消息中心与开发工具之间（sort 7，开发工具 sort 8）。
--    组织部门「业务中心」(sys_dept) 不变，仅改菜单 sys_menu id=8 / id=150。
-- #7 系统监控下新增「缓存监控」菜单（Redis INFO 图表 + SCAN 键管理）。
-- #8 系统监控下新增「服务监控」菜单（本机 JMX：CPU / 内存 / JVM / 磁盘）。
-- #9 群成员表新增 notify_muted（免打扰仅@提醒）、announcement_read_time（群公告已读）。
-- 库名 wuadmin，与 application-prod.yml 一致。
--
-- 用法: mysql -u wuadmin -p wuadmin < sql/add6_7_wuadmin.sql
-- 执行后请重新登录以刷新侧栏菜单与权限。
-- =============================================================================

USE `wuadmin`;

-- ---------- add6：流程中心 ----------
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

-- ---------- add7：缓存监控 ----------
INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status)
VALUES (185, '缓存监控', 'monitor:cache:list', 2, 4, 100, '/monitor/cache', 'Coin', 'monitor/cache/index', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    permission = VALUES(permission),
    type = VALUES(type),
    sort = VALUES(sort),
    parent_id = VALUES(parent_id),
    path = VALUES(path),
    icon = VALUES(icon),
    component = VALUES(component),
    status = VALUES(status);

INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status)
VALUES (186, '缓存删除', 'monitor:cache:delete', 3, 1, 185, '', '', '', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    permission = VALUES(permission),
    type = VALUES(type),
    sort = VALUES(sort),
    parent_id = VALUES(parent_id),
    status = VALUES(status);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 185),
(1, 186);

-- ---------- add8：服务监控 ----------
INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status)
VALUES (187, '服务监控', 'monitor:server:list', 2, 5, 100, '/monitor/server', 'Cpu', 'monitor/server/index', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    permission = VALUES(permission),
    type = VALUES(type),
    sort = VALUES(sort),
    parent_id = VALUES(parent_id),
    path = VALUES(path),
    icon = VALUES(icon),
    component = VALUES(component),
    status = VALUES(status);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 187);

-- ---------- add9：群成员免打扰 / 群公告已读 ----------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_chat_group_member'
      AND COLUMN_NAME = 'notify_muted'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE sys_chat_group_member ADD COLUMN notify_muted TINYINT DEFAULT 0 COMMENT ''0正常 1免打扰仅@提醒'' AFTER muted',
    'SELECT ''notify_muted exists'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_chat_group_member'
      AND COLUMN_NAME = 'announcement_read_time'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE sys_chat_group_member ADD COLUMN announcement_read_time DATETIME NULL COMMENT ''群公告已读时间'' AFTER notify_muted',
    'SELECT ''announcement_read_time exists'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT '[OK] add6_7_wuadmin.sql finished' AS result;

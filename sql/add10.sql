-- =============================================================================
-- add10.sql  增量补丁 #10（可重复执行）
-- =============================================================================
-- 回收中心：
--   1. 系统管理下新增菜单「回收中心」(id=163, system:recycle:list)
--   2. sys_file 增加软删除字段（文件回收）
-- 用法: mysql -u root -p wu-admin < sql/add10.sql
-- 生产/服务器: mysql -u wuadmin -p wuadmin < sql/add10_wuadmin.sql
-- 执行后请重新登录以刷新侧栏菜单与权限。
-- =============================================================================

USE `wu-admin`;

-- ---------- 回收中心菜单 ----------
INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status)
VALUES (163, '回收中心', 'system:recycle:list', 2, 7, 1, '/system/recycle', 'Delete', 'system/recycle/index', 1)
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

UPDATE sys_menu SET sort = 8 WHERE id = 160 AND parent_id = 1 AND deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 163);

-- ---------- sys_file 软删除（先 update_time，再 deleted，避免列顺序错乱）----------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_file'
      AND COLUMN_NAME = 'update_time'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE sys_file ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER create_time',
    'SELECT ''update_time exists'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_file'
      AND COLUMN_NAME = 'deleted'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE sys_file ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT ''是否删除'' AFTER update_time',
    'SELECT ''deleted exists'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_file SET deleted = 0 WHERE deleted IS NULL;
UPDATE sys_file SET update_time = IFNULL(update_time, create_time) WHERE update_time IS NULL;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_file'
      AND INDEX_NAME = 'idx_deleted_update'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE sys_file ADD INDEX idx_deleted_update (deleted, update_time)',
    'SELECT ''idx_deleted_update exists'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT '[OK] add10.sql finished' AS result;

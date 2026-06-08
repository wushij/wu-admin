-- =============================================================================
-- add11_wuadmin.sql  生产/服务器增量补丁 #11 + #12（可重复执行，MySQL 5.6+）
-- =============================================================================
-- #11 代码生成：gen_table / gen_table_column + 开发工具菜单（164-169,179）
-- #12 gen_table.table_name 唯一约束（防重复导入；兼容旧版 add11 仅普通索引）
--
-- 用法: mysql -u wuadmin -p wuadmin < sql/add11_wuadmin.sql
-- 执行后请重新登录以刷新侧栏菜单与权限。
-- =============================================================================

USE `wuadmin`;
SET NAMES utf8mb4;

-- ---------- add11：代码生成表 ----------
CREATE TABLE IF NOT EXISTS gen_table (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
  table_name VARCHAR(191) DEFAULT '' COMMENT '表名称（191 适配 MySQL5.6 utf8mb4 唯一索引 767 字节上限）',
  table_comment VARCHAR(500) DEFAULT '' COMMENT '表描述',
  class_name VARCHAR(100) DEFAULT '' COMMENT '实体类名称',
  package_name VARCHAR(100) DEFAULT '' COMMENT '生成包路径',
  module_name VARCHAR(30) DEFAULT '' COMMENT '生成模块名',
  business_name VARCHAR(30) DEFAULT '' COMMENT '生成业务名',
  function_name VARCHAR(50) DEFAULT '' COMMENT '生成功能名',
  author VARCHAR(50) DEFAULT '' COMMENT '作者',
  gen_type VARCHAR(10) DEFAULT 'crud' COMMENT '生成类型',
  gen_path VARCHAR(200) DEFAULT '/' COMMENT '生成路径',
  front_type VARCHAR(30) DEFAULT 'element-plus' COMMENT '前端模板',
  form_layout VARCHAR(20) DEFAULT 'vertical' COMMENT '表单布局',
  parent_menu_id BIGINT DEFAULT NULL COMMENT '上级菜单ID',
  remark VARCHAR(500) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE INDEX uk_gen_table_name (table_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成业务表';

CREATE TABLE IF NOT EXISTS gen_table_column (
  id BIGINT NOT NULL AUTO_INCREMENT,
  table_id BIGINT DEFAULT NULL,
  column_name VARCHAR(200) DEFAULT '',
  column_comment VARCHAR(500) DEFAULT '',
  column_type VARCHAR(100) DEFAULT '',
  java_type VARCHAR(50) DEFAULT '',
  java_field VARCHAR(200) DEFAULT '',
  is_pk TINYINT DEFAULT 0,
  is_increment TINYINT DEFAULT 0,
  is_required TINYINT DEFAULT 0,
  is_insert TINYINT DEFAULT 0,
  is_edit TINYINT DEFAULT 0,
  is_list TINYINT DEFAULT 0,
  is_query TINYINT DEFAULT 0,
  query_type VARCHAR(20) DEFAULT 'EQ',
  html_type VARCHAR(50) DEFAULT '',
  dict_type VARCHAR(100) DEFAULT '',
  sort INT DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_gen_col_table (table_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成字段表';

-- ---------- add11：代码生成菜单 ----------
INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status)
VALUES (164, '代码生成', 'tool:gen:list', 2, 2, 150, '/tool/gen', 'DocumentCopy', 'tool/gen/index', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), permission = VALUES(permission), type = VALUES(type),
    sort = VALUES(sort), parent_id = VALUES(parent_id), path = VALUES(path),
    icon = VALUES(icon), component = VALUES(component), status = VALUES(status);

INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status) VALUES
(165, '代码生成查询', 'tool:gen:query', 3, 1, 164, '', '', '', 1),
(166, '代码生成导入', 'tool:gen:import', 3, 2, 164, '', '', '', 1),
(167, '代码生成修改', 'tool:gen:edit', 3, 3, 164, '', '', '', 1),
(168, '代码生成删除', 'tool:gen:remove', 3, 4, 164, '', '', '', 1),
(169, '代码生成预览', 'tool:gen:preview', 3, 5, 164, '', '', '', 1),
(179, '代码生成执行', 'tool:gen:code', 3, 6, 164, '', '', '', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), permission = VALUES(permission);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 164), (1, 165), (1, 166), (1, 167), (1, 168), (1, 169), (1, 179);

-- ---------- add12：唯一索引升级（旧库若仅有 idx_gen_table_name 则迁移） ----------
DELETE c FROM gen_table_column c
INNER JOIN gen_table t ON c.table_id = t.id
INNER JOIN gen_table dup ON dup.table_name = t.table_name AND dup.id < t.id;

DELETE t1 FROM gen_table t1
INNER JOIN gen_table t2 ON t1.table_name = t2.table_name AND t1.id > t2.id;

DROP PROCEDURE IF EXISTS sp_drop_index_if_exists;
DROP PROCEDURE IF EXISTS sp_add_unique_index_if_not_exists;

DELIMITER $$

CREATE PROCEDURE sp_drop_index_if_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64)
)
BEGIN
    DECLARE v_cnt INT DEFAULT 0;

    SELECT COUNT(*) INTO v_cnt
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table
      AND index_name = p_index;

    IF v_cnt > 0 THEN
        SET @ddl_sql = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_index, '`');
        PREPARE stmt FROM @ddl_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('[DROP] ', p_table, '.', p_index) AS result;
    ELSE
        SELECT CONCAT('[SKIP] ', p_table, '.', p_index) AS result;
    END IF;
END$$

CREATE PROCEDURE sp_add_unique_index_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_columns VARCHAR(255)
)
BEGIN
    DECLARE v_cnt INT DEFAULT 0;

    SELECT COUNT(*) INTO v_cnt
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table
      AND index_name = p_index;

    IF v_cnt = 0 THEN
        SET @ddl_sql = CONCAT('ALTER TABLE `', p_table, '` ADD UNIQUE INDEX `', p_index, '` (', p_columns, ')');
        PREPARE stmt FROM @ddl_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('[ADD] ', p_table, '.', p_index) AS result;
    ELSE
        SELECT CONCAT('[SKIP] ', p_table, '.', p_index) AS result;
    END IF;
END$$

DELIMITER ;

CALL sp_drop_index_if_exists('gen_table', 'idx_gen_table_name');
CALL sp_drop_index_if_exists('gen_table', 'uk_gen_table_name');
CALL sp_add_unique_index_if_not_exists('gen_table', 'uk_gen_table_name', 'table_name');

DROP PROCEDURE IF EXISTS sp_drop_index_if_exists;
DROP PROCEDURE IF EXISTS sp_add_unique_index_if_not_exists;

SELECT '[OK] add11_wuadmin.sql finished (#11+#12)' AS result;

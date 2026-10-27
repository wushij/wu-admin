-- =============================================================================
-- add16 · 菜单图标对齐与系统管理菜单排序优化
-- -----------------------------------------------------------------------------
-- 优化内容：
-- 1. 系统日志一级目录使用日历图标 Calendar（对齐设计规范）
-- 2. AI 模型配置菜单使用现代 AI 智能微光星芒图标 AiModel（替换呆板芯片，凸显大模型科技美感）
-- 3. 服务监控菜单使用实时仪表盘图标 Odometer（替换原 CPU 芯片，凸显性能监控与负载指标）
-- 4. 安全风控菜单使用经典安全锁图标 Lock（替换难看的实心盾牌，清爽耐看）
-- 5. 流程中心一级目录使用工作流分支流转图标 Workflow（替换原不贴合的滑块）
-- 6. 系统管理子菜单顺序重排：
--    用户管理(1) -> 角色管理(2) -> 菜单管理(3) -> 组织管理(4)
--    -> 字典管理(5) -> 回收中心(6) -> 系统配置(7)
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `wu-admin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `wu-admin`;

SET NAMES utf8mb4;

-- 1. 系统日志菜单图标更新为 Calendar
UPDATE `sys_menu`
SET `icon` = 'Calendar'
WHERE `id` = 120 OR `path` = '/log';

-- 2. AI 模型配置菜单图标更新为 AiModel（现代 AI 智能微光星芒）
UPDATE `sys_menu`
SET `icon` = 'AiModel'
WHERE `id` = 200 OR `path` = '/ai/model';

-- 3. 服务监控菜单图标更新为 Odometer（实时性能负载仪表盘）
UPDATE `sys_menu`
SET `icon` = 'Odometer'
WHERE `id` = 187 OR `path` = '/monitor/server';

-- 4. 安全风控菜单图标更新为 Lock（经典安全锁）
UPDATE `sys_menu`
SET `icon` = 'Lock'
WHERE `id` = 230 OR `path` = '/system/security-log';

-- 5. 流程中心菜单图标更新为 Workflow（业务工作流分支流转）
UPDATE `sys_menu`
SET `icon` = 'Workflow'
WHERE `id` = 8 OR `path` = '/workflow';

-- 6. 系统管理子菜单顺序重排
UPDATE `sys_menu` SET `sort` = 1 WHERE `id` = 2;   -- 用户管理
UPDATE `sys_menu` SET `sort` = 2 WHERE `id` = 3;   -- 角色管理
UPDATE `sys_menu` SET `sort` = 3 WHERE `id` = 4;   -- 菜单管理
UPDATE `sys_menu` SET `sort` = 4 WHERE `id` = 5;   -- 组织管理
UPDATE `sys_menu` SET `sort` = 5 WHERE `id` = 130; -- 字典管理
UPDATE `sys_menu` SET `sort` = 6 WHERE `id` = 163; -- 回收中心
UPDATE `sys_menu` SET `sort` = 7 WHERE `id` = 160; -- 系统配置

-- 7. 校验查询验证
SELECT `id`, `parent_id`, `name`, `sort`, `icon`, `path`
FROM `sys_menu`
WHERE `parent_id` = 1
ORDER BY `sort` ASC, `id` ASC;

SELECT `id`, `name`, `icon`, `path`
FROM `sys_menu`
WHERE `id` IN (8, 120, 129, 187, 200, 230)
   OR `path` IN ('/workflow', '/log', '/ai/model', '/monitor/server', '/system/security-log');

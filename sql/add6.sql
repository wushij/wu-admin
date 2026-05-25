-- =============================================================================
-- add6.sql — 组织管理示例数据增量（部门 + 岗位）
--
-- 用法：
--   mysql -u root -p wu-admin < sql/add6.sql
--
-- 说明：
--   - 可重复执行：按部门名称 / 岗位编码判重，已存在则跳过
--   - 无 DROP，不删改已有业务数据
--   - 依赖总公司(id=1)、研发部(id=2) 等 admin_platform 初始数据
-- =============================================================================

USE `wu-admin`;

-- -----------------------------------------------------------------------------
-- 1. 一级部门（隶属总公司 parent_id = 1）
-- -----------------------------------------------------------------------------
INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '实训部', 1, '0,1', 4, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '实训部' AND parent_id = 1 AND deleted = 0);

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '人事部', 1, '0,1', 5, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '人事部' AND parent_id = 1 AND deleted = 0);

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '运维部', 1, '0,1', 6, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '运维部' AND parent_id = 1 AND deleted = 0);

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '产品部', 1, '0,1', 7, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '产品部' AND parent_id = 1 AND deleted = 0);

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '客服部', 1, '0,1', 8, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '客服部' AND parent_id = 1 AND deleted = 0);

-- -----------------------------------------------------------------------------
-- 2. 二级部门（隶属研发部）
-- -----------------------------------------------------------------------------
INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '前端组', d.id, CONCAT(d.ancestors, ',', d.id), 1, 1, NULL
FROM sys_dept d
WHERE d.name = '研发部' AND d.parent_id = 1 AND d.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_dept c WHERE c.name = '前端组' AND c.parent_id = d.id AND c.deleted = 0
  )
LIMIT 1;

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '后端组', d.id, CONCAT(d.ancestors, ',', d.id), 2, 1, NULL
FROM sys_dept d
WHERE d.name = '研发部' AND d.parent_id = 1 AND d.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_dept c WHERE c.name = '后端组' AND c.parent_id = d.id AND c.deleted = 0
  )
LIMIT 1;

-- -----------------------------------------------------------------------------
-- 3. 岗位（顶级 + 子岗位）
-- -----------------------------------------------------------------------------
INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT 0, 'qa', '测试', 2, 1, '' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'qa' AND deleted = 0);

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'qa_lead', '测试组长', 0, 1, '隶属测试'
FROM sys_post p
WHERE p.post_code = 'qa' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'qa_lead' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT 0, 'market_spec', '市场专员', 3, 1, '' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'market_spec' AND deleted = 0);

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT 0, 'finance_mgr', '财务主管', 4, 1, '' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'finance_mgr' AND deleted = 0);

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT 0, 'train_lecturer', '实训讲师', 5, 1, '' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'train_lecturer' AND deleted = 0);

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT 0, 'hr_spec', '人事专员', 6, 1, '' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'hr_spec' AND deleted = 0);

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT 0, 'ops_eng', '运维工程师', 7, 1, '' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'ops_eng' AND deleted = 0);

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT 0, 'product_mgr', '产品经理', 8, 1, '' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'product_mgr' AND deleted = 0);

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'fe_dev', '前端开发', 0, 1, '隶属研发工程师'
FROM sys_post p
WHERE p.post_code = 'dev' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'fe_dev' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'be_dev', '后端开发', 1, 1, '隶属研发工程师'
FROM sys_post p
WHERE p.post_code = 'dev' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'be_dev' AND deleted = 0)
LIMIT 1;

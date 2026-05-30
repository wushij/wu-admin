-- =============================================================================
-- add6.sql — 组织管理示例数据增量（部门 + 岗位）
--
-- 用法：mysql -u root -p wu-admin < sql/add6.sql
-- 说明：可重复执行；新库请先执行 admin_platform 或 add9.sql 建立分级结构
-- =============================================================================

USE `wu-admin`;

-- 1. 二级中心（隶属本部 id=1）
INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '技术中心', 1, '0,1', 1, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '技术中心' AND parent_id = 1 AND deleted = 0);

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '业务中心', 1, '0,1', 2, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '业务中心' AND parent_id = 1 AND deleted = 0);

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '职能中心', 1, '0,1', 3, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '职能中心' AND parent_id = 1 AND deleted = 0);

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '运营中心', 1, '0,1', 4, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '运营中心' AND parent_id = 1 AND deleted = 0);

-- 2. 三级部门（按中心归类，仅当仍挂在根下时插入/迁移由 add9 处理）
INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '实训部', c.id, CONCAT(c.ancestors, ',', c.id), 1, 1, NULL
FROM sys_dept c
WHERE c.name = '运营中心' AND c.parent_id = 1 AND c.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '实训部' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '人事部', c.id, CONCAT(c.ancestors, ',', c.id), 2, 1, NULL
FROM sys_dept c
WHERE c.name = '职能中心' AND c.parent_id = 1 AND c.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '人事部' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '运维部', c.id, CONCAT(c.ancestors, ',', c.id), 2, 1, NULL
FROM sys_dept c
WHERE c.name = '技术中心' AND c.parent_id = 1 AND c.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '运维部' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '产品部', c.id, CONCAT(c.ancestors, ',', c.id), 3, 1, NULL
FROM sys_dept c
WHERE c.name = '技术中心' AND c.parent_id = 1 AND c.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '产品部' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '客服部', c.id, CONCAT(c.ancestors, ',', c.id), 2, 1, NULL
FROM sys_dept c
WHERE c.name = '运营中心' AND c.parent_id = 1 AND c.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '客服部' AND deleted = 0)
LIMIT 1;

-- 3. 研发部下的组
INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '前端组', d.id, CONCAT(d.ancestors, ',', d.id), 1, 1, NULL
FROM sys_dept d
WHERE d.name = '研发部' AND d.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_dept c WHERE c.name = '前端组' AND c.parent_id = d.id AND c.deleted = 0)
LIMIT 1;

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '后端组', d.id, CONCAT(d.ancestors, ',', d.id), 2, 1, NULL
FROM sys_dept d
WHERE d.name = '研发部' AND d.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_dept c WHERE c.name = '后端组' AND c.parent_id = d.id AND c.deleted = 0)
LIMIT 1;

-- 4. 岗位（董事长 → 总经理 → 分类/专业岗位）
INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT 0, 'chairman', '董事长', 0, 1, '岗位体系根' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'chairman' AND deleted = 0);

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'qa', '测试', 2, 1, ''
FROM sys_post p
WHERE p.post_code = 'cto' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'qa' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'qa_lead', '测试组长', 0, 1, '隶属测试'
FROM sys_post p
WHERE p.post_code = 'qa' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'qa_lead' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'market_spec', '市场专员', 1, 1, ''
FROM sys_post p
WHERE p.post_code = 'biz_line' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'market_spec' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'finance_mgr', '财务主管', 1, 1, ''
FROM sys_post p
WHERE p.post_code = 'func_line' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'finance_mgr' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'train_lecturer', '实训讲师', 1, 1, ''
FROM sys_post p
WHERE p.post_code = 'ops_line' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'train_lecturer' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'hr_spec', '人事专员', 2, 1, ''
FROM sys_post p
WHERE p.post_code = 'func_line' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'hr_spec' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'ops_eng', '运维工程师', 4, 1, ''
FROM sys_post p
WHERE p.post_code = 'cto' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'ops_eng' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'product_mgr', '产品经理', 3, 1, ''
FROM sys_post p
WHERE p.post_code = 'cto' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'product_mgr' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'fe_dev', '前端开发', 1, 1, '隶属研发工程师'
FROM sys_post p
WHERE p.post_code = 'dev' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'fe_dev' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'be_dev', '后端开发', 2, 1, '隶属研发工程师'
FROM sys_post p
WHERE p.post_code = 'dev' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'be_dev' AND deleted = 0)
LIMIT 1;

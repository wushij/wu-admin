-- =============================================================================
-- add9.sql — 组织树分级重组（部门中心 + 岗位层级，去掉「总公司」展示名）
--
-- 用法：mysql -u root -p wu-admin < sql/add9.sql
-- 可重复执行：按名称/编码判重；已归类的部门/岗位会跳过
-- =============================================================================

USE `wu-admin`;

-- 1. 根节点改名（库内保留 id=1 虚拟根，界面不展示「总公司」）
UPDATE sys_dept SET name = '本部' WHERE id = 1 AND name = '总公司' AND deleted = 0;

-- 2. 二级「中心」分类
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

-- 3. 原一级部门归入各中心（仅当仍直接挂在根节点下时调整）
UPDATE sys_dept d
INNER JOIN sys_dept c ON c.name = '技术中心' AND c.parent_id = 1 AND c.deleted = 0
SET d.parent_id = c.id, d.ancestors = CONCAT(c.ancestors, ',', c.id)
WHERE d.name IN ('研发部', '运维部', '产品部') AND d.deleted = 0 AND d.parent_id = 1;

UPDATE sys_dept d
INNER JOIN sys_dept c ON c.name = '业务中心' AND c.parent_id = 1 AND c.deleted = 0
SET d.parent_id = c.id, d.ancestors = CONCAT(c.ancestors, ',', c.id)
WHERE d.name = '市场部' AND d.deleted = 0 AND d.parent_id = 1;

UPDATE sys_dept d
INNER JOIN sys_dept c ON c.name = '职能中心' AND c.parent_id = 1 AND c.deleted = 0
SET d.parent_id = c.id, d.ancestors = CONCAT(c.ancestors, ',', c.id)
WHERE d.name IN ('财务部', '人事部') AND d.deleted = 0 AND d.parent_id = 1;

UPDATE sys_dept d
INNER JOIN sys_dept c ON c.name = '运营中心' AND c.parent_id = 1 AND c.deleted = 0
SET d.parent_id = c.id, d.ancestors = CONCAT(c.ancestors, ',', c.id)
WHERE d.name IN ('实训部', '客服部') AND d.deleted = 0 AND d.parent_id = 1;

-- 4. 研发部下级组 ancestors 修正
UPDATE sys_dept g
INNER JOIN sys_dept rd ON rd.name = '研发部' AND rd.deleted = 0
SET g.parent_id = rd.id, g.ancestors = CONCAT(rd.ancestors, ',', rd.id)
WHERE g.name IN ('前端组', '后端组') AND g.deleted = 0
  AND (g.parent_id <> rd.id OR g.ancestors NOT LIKE CONCAT(rd.ancestors, ',', rd.id, '%'));

-- 5. 岗位层级：董事长 → 总经理 → 技术总监 / 开发工程师 / 业务·职能·运营体系
INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT 0, 'chairman', '董事长', 0, 1, '岗位体系根' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'chairman' AND deleted = 0);

UPDATE sys_post ceo
INNER JOIN sys_post ch ON ch.post_code = 'chairman' AND ch.deleted = 0
SET ceo.parent_id = ch.id, ceo.sort = 1
WHERE ceo.post_code = 'ceo' AND ceo.deleted = 0 AND ceo.parent_id = 0;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'cto', '技术总监', 1, 1, ''
FROM sys_post p
WHERE p.post_code = 'ceo' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'cto' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'dev_exec', '开发工程师', 2, 1, ''
FROM sys_post p
WHERE p.post_code = 'ceo' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'dev_exec' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'biz_line', '业务体系', 3, 1, '岗位分类' FROM sys_post p
WHERE p.post_code = 'ceo' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'biz_line' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'func_line', '职能体系', 4, 1, '岗位分类' FROM sys_post p
WHERE p.post_code = 'ceo' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'func_line' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'ops_line', '运营体系', 5, 1, '岗位分类' FROM sys_post p
WHERE p.post_code = 'ceo' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'ops_line' AND deleted = 0)
LIMIT 1;

UPDATE sys_post x
INNER JOIN sys_post cto ON cto.post_code = 'cto' AND cto.deleted = 0
SET x.parent_id = cto.id
WHERE x.post_code IN ('dev', 'qa', 'product_mgr', 'ops_eng') AND x.deleted = 0 AND x.parent_id = 0;

UPDATE sys_post x
INNER JOIN sys_post bl ON bl.post_code = 'biz_line' AND bl.deleted = 0
SET x.parent_id = bl.id
WHERE x.post_code = 'market_spec' AND x.deleted = 0 AND x.parent_id = 0;

UPDATE sys_post x
INNER JOIN sys_post fl ON fl.post_code = 'func_line' AND fl.deleted = 0
SET x.parent_id = fl.id
WHERE x.post_code IN ('finance_mgr', 'hr_spec') AND x.deleted = 0 AND x.parent_id = 0;

UPDATE sys_post x
INNER JOIN sys_post ol ON ol.post_code = 'ops_line' AND ol.deleted = 0
SET x.parent_id = ol.id
WHERE x.post_code = 'train_lecturer' AND x.deleted = 0 AND x.parent_id = 0;

UPDATE sys_post x
INNER JOIN sys_post dev ON dev.post_code = 'dev' AND dev.deleted = 0
SET x.parent_id = dev.id
WHERE x.post_code IN ('dev_lead', 'fe_dev', 'be_dev') AND x.deleted = 0 AND x.parent_id <> dev.id;

UPDATE sys_post x
INNER JOIN sys_post qa ON qa.post_code = 'qa' AND qa.deleted = 0
SET x.parent_id = qa.id
WHERE x.post_code = 'qa_lead' AND x.deleted = 0 AND x.parent_id <> qa.id;

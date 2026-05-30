-- add15.sql — 字典扩展：工单、审批业务字典
USE `wu-admin`;
SET NAMES utf8mb4;

INSERT INTO sys_dict_type (id, dict_name, dict_type, status, remark) VALUES
(4, '工单状态', 'sys_ticket_status', 1, '工单流转状态'),
(5, '工单优先级', 'sys_ticket_priority', 1, '工单优先级'),
(6, '审批类型', 'sys_approval_form_type', 1, '审批单业务类型'),
(7, '审批状态', 'sys_approval_status', 1, '审批单流转状态')
ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name), remark = VALUES(remark);

INSERT INTO sys_dict_data (dict_type, sort, dict_label, dict_value, list_class, is_default, status) VALUES
('sys_ticket_status', 1, '待处理', 'OPEN', 'info', 1, 1),
('sys_ticket_status', 2, '处理中', 'IN_PROGRESS', 'warning', 0, 1),
('sys_ticket_status', 3, '已解决', 'RESOLVED', 'success', 0, 1),
('sys_ticket_status', 4, '已关闭', 'CLOSED', 'danger', 0, 1),
('sys_ticket_priority', 1, '低', 'LOW', 'info', 0, 1),
('sys_ticket_priority', 2, '中', 'MEDIUM', 'success', 1, 1),
('sys_ticket_priority', 3, '高', 'HIGH', 'warning', 0, 1),
('sys_ticket_priority', 4, '紧急', 'URGENT', 'danger', 0, 1),
('sys_approval_form_type', 1, '通用', 'GENERAL', 'info', 1, 1),
('sys_approval_form_type', 2, '请假', 'LEAVE', 'primary', 0, 1),
('sys_approval_form_type', 3, '采购', 'PURCHASE', 'warning', 0, 1),
('sys_approval_form_type', 4, '报销', 'REIMBURSE', 'success', 0, 1),
('sys_approval_form_type', 5, '用印', 'SEAL', 'danger', 0, 1),
('sys_approval_form_type', 6, '合同', 'CONTRACT', 'info', 0, 1),
('sys_approval_form_type', 7, '注册审核', 'REGISTER', 'warning', 0, 1),
('sys_approval_status', 1, '待审批', 'SUBMITTED', 'warning', 1, 1),
('sys_approval_status', 2, '已通过', 'APPROVED', 'success', 0, 1),
('sys_approval_status', 3, '已驳回', 'REJECTED', 'danger', 0, 1),
('sys_approval_status', 4, '已归档', 'ARCHIVED', 'info', 0, 1)
ON DUPLICATE KEY UPDATE
    dict_label = VALUES(dict_label),
    list_class = VALUES(list_class),
    is_default = VALUES(is_default),
    sort = VALUES(sort);

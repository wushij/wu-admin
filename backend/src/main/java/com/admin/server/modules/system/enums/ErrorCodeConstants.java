package com.admin.server.modules.system.enums;

import com.admin.server.common.exception.ErrorCode;

/**
 * 系统模块与全局错误码常量定义
 * <p>
 * 编号规则：1_{模块 2位}_{业务 2位}_{错误 3位}
 * </p>
 */
public interface ErrorCodeConstants {

    // ========== 全局级错误码 (400 - 500) ==========
    ErrorCode BAD_REQUEST = new ErrorCode(400, "请求参数不合法");
    ErrorCode UNAUTHORIZED = new ErrorCode(401, "未登录或登录已超时");
    ErrorCode FORBIDDEN = new ErrorCode(403, "没有操作权限");
    ErrorCode NOT_FOUND = new ErrorCode(404, "资源不存在");
    ErrorCode INTERNAL_SERVER_ERROR = new ErrorCode(500, "系统内部异常");

    // ========== System 模块 - 用户 (1_01_01_xxx) ==========
    ErrorCode USER_NOT_EXISTS = new ErrorCode(1_01_01_001, "用户不存在");
    ErrorCode USER_USERNAME_EXISTS = new ErrorCode(1_01_01_002, "用户名已存在");
    ErrorCode USER_MOBILE_EXISTS = new ErrorCode(1_01_01_003, "手机号已被使用");
    ErrorCode USER_EMAIL_EXISTS = new ErrorCode(1_01_01_004, "邮箱已被使用");
    ErrorCode USER_PASSWORD_FAILED = new ErrorCode(1_01_01_005, "密码错误");
    ErrorCode USER_DISABLED = new ErrorCode(1_01_01_006, "账号已被禁用");
    ErrorCode USER_OLD_PASSWORD_ERROR = new ErrorCode(1_01_01_007, "原密码输入错误");
    ErrorCode USER_CANNOT_DELETE_SELF = new ErrorCode(1_01_01_008, "不能删除当前登录账号");

    // ========== System 模块 - 角色/部门/岗位/菜单/字典 (1_01_02_xxx) ==========
    ErrorCode ROLE_NOT_EXISTS = new ErrorCode(1_01_02_001, "角色不存在");
    ErrorCode ROLE_CODE_EXISTS = new ErrorCode(1_01_02_002, "角色编码已存在");
    ErrorCode DEPT_NOT_EXISTS = new ErrorCode(1_01_02_003, "部门不存在");
    ErrorCode DEPT_HAS_CHILDREN = new ErrorCode(1_01_02_004, "存在子部门，不允许删除");
    ErrorCode POST_NOT_EXISTS = new ErrorCode(1_01_02_005, "岗位不存在");
    ErrorCode MENU_NOT_EXISTS = new ErrorCode(1_01_02_006, "菜单不存在");
    ErrorCode DICT_TYPE_NOT_EXISTS = new ErrorCode(1_01_02_007, "字典类型不存在");
    ErrorCode DICT_DATA_NOT_EXISTS = new ErrorCode(1_01_02_008, "字典数据不存在");

    // ========== Infra 模块 - 文件/配置/任务/生成 (1_02_xx_xxx) ==========
    ErrorCode FILE_IS_EMPTY = new ErrorCode(1_02_01_001, "上传文件不能为空");
    ErrorCode FILE_EXCEED_MAX_SIZE = new ErrorCode(1_02_01_002, "文件大小超出允许范围");
    ErrorCode FILE_TYPE_NOT_ALLOWED = new ErrorCode(1_02_01_003, "文件扩展名不在允许白名单内");
    ErrorCode JOB_NOT_EXISTS = new ErrorCode(1_02_02_001, "定时任务不存在");
    ErrorCode GEN_TABLE_NOT_EXISTS = new ErrorCode(1_02_03_001, "代码生成表配置不存在");

    // ========== Ticket 模块 - 工单/审批 (1_03_01_xxx) ==========
    ErrorCode TICKET_NOT_EXISTS = new ErrorCode(1_03_01_001, "工单不存在");
    ErrorCode TICKET_STATUS_INVALID = new ErrorCode(1_03_01_002, "工单当前状态无法执行此流转");
    ErrorCode APPROVAL_FORM_NOT_EXISTS = new ErrorCode(1_03_01_003, "审批表单不存在");

    // ========== Message 模块 - 消息/聊天/公告 (1_04_01_xxx) ==========
    ErrorCode NOTICE_NOT_EXISTS = new ErrorCode(1_04_01_001, "通知公告不存在");
    ErrorCode CHAT_MESSAGE_NOT_EXISTS = new ErrorCode(1_04_01_002, "消息记录不存在");

    // ========== Trade 模块 - 支付/短信/邮箱 (1_05_01_xxx) ==========
    ErrorCode CAPTCHA_CODE_ERROR = new ErrorCode(1_05_01_001, "验证码错误或已过期");
    ErrorCode CAPTCHA_SLIDER_FAIL = new ErrorCode(1_05_01_002, "滑块验证失败，请重新拖动");
    ErrorCode SMS_SEND_FREQ_LIMIT = new ErrorCode(1_05_01_003, "短信发送过于频繁，请稍后再试");
    ErrorCode EMAIL_SEND_FREQ_LIMIT = new ErrorCode(1_05_01_004, "邮件发送过于频繁，请稍后再试");
    ErrorCode PAY_ORDER_NOT_EXISTS = new ErrorCode(1_05_02_001, "支付订单不存在");
}

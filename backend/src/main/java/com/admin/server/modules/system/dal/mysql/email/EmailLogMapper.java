package com.admin.server.modules.system.dal.mysql.email;

import com.admin.server.modules.system.dal.dataobject.email.EmailLogDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EmailLogMapper extends BaseMapper<EmailLogDO> {

    @Update("CREATE TABLE IF NOT EXISTS sys_email_log (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "email VARCHAR(100) NOT NULL COMMENT '接收邮箱', " +
            "subject VARCHAR(200) COMMENT '邮件主题', " +
            "content VARCHAR(500) COMMENT '验证码或邮件摘要', " +
            "scene VARCHAR(50) COMMENT '业务场景', " +
            "provider VARCHAR(50) COMMENT '发件服务商', " +
            "status TINYINT DEFAULT 1 COMMENT '状态(1-成功 2-失败)', " +
            "result_msg VARCHAR(1000) COMMENT '结果明细', " +
            "ip VARCHAR(50) COMMENT '请求IP', " +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件发送记录表'")
    void createTableIfNotExists();
}

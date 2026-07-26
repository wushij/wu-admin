package com.admin.server.modules.trade.service.email.impl;

import com.admin.server.common.util.ClientIpUtils;
import com.admin.server.modules.trade.dal.dataobject.email.EmailLogDO;
import com.admin.server.modules.trade.dal.mysql.email.EmailLogMapper;
import com.admin.server.modules.trade.service.email.EmailLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Service
public class EmailLogServiceImpl extends ServiceImpl<EmailLogMapper, EmailLogDO> implements EmailLogService {

    @PostConstruct
    public void initTable() {
        try {
            getBaseMapper().createTableIfNotExists();
        } catch (Exception e) {
            log.warn("Auto create sys_email_log table notice: {}", e.getMessage());
        }
    }

    @Override
    public void recordLog(String email, String subject, String content, String scene,
                           String provider, boolean success, String resultMsg, String ip) {
        try {
            if (ip == null) {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                HttpServletRequest request = attrs != null ? attrs.getRequest() : null;
                if (request != null) {
                    ip = ClientIpUtils.resolve(request);
                }
            }

            EmailLogDO logDO = new EmailLogDO();
            logDO.setEmail(email);
            logDO.setSubject(subject);
            logDO.setContent(content);
            logDO.setScene(scene);
            logDO.setProvider(provider);
            logDO.setStatus(success ? 1 : 2);
            logDO.setResultMsg(resultMsg);
            logDO.setIp(ip);
            logDO.setCreateTime(LocalDateTime.now());
            save(logDO);
        } catch (Exception e) {
            log.error("保存邮件日志失败: {}", e.getMessage(), e);
        }
    }
}

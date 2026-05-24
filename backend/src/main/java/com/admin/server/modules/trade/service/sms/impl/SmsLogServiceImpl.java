package com.admin.server.modules.trade.service.sms.impl;

import com.admin.server.common.util.ClientIpUtils;
import com.admin.server.modules.trade.dal.dataobject.sms.SmsLogDO;
import com.admin.server.modules.trade.dal.mysql.sms.SmsLogMapper;
import com.admin.server.modules.trade.service.sms.SmsLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
public class SmsLogServiceImpl extends ServiceImpl<SmsLogMapper, SmsLogDO> implements SmsLogService {

    @Override
    public void log(String phone, String content, String smsType, String templateId,
                    String templateParams, String provider, boolean success,
                    String resultMsg, String bizId, Long userId, String bizType, String ip) {
        SmsLogDO smsLog = new SmsLogDO();
        smsLog.setPhone(phone);
        smsLog.setContent(content);
        smsLog.setSmsType(smsType);
        smsLog.setTemplateId(templateId);
        smsLog.setTemplateParams(templateParams);
        smsLog.setProvider(provider);
        smsLog.setStatus(success ? 1 : 2);
        smsLog.setResultMsg(resultMsg);
        smsLog.setBizId(bizId);
        smsLog.setSendTime(LocalDateTime.now());
        smsLog.setUserId(userId);
        smsLog.setBizType(bizType);
        smsLog.setIp(ip);
        save(smsLog);
    }

    @Override
    public void logVerifyCode(String phone, String code, String provider, boolean success, String resultMsg, String bizId) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs != null ? attrs.getRequest() : null;
        String ip = request != null ? ClientIpUtils.resolve(request) : null;
        log(phone, code, "verify_code", null, null, provider, success, resultMsg, bizId, null, "login", ip);
    }
}

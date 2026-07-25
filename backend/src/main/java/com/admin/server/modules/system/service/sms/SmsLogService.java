package com.admin.server.modules.system.service.sms;

import com.admin.server.modules.system.dal.dataobject.sms.SmsLogDO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface SmsLogService extends IService<SmsLogDO> {

    void log(String phone, String content, String smsType, String templateId,
             String templateParams, String provider, boolean success,
             String resultMsg, String bizId, Long userId, String bizType, String ip);

    void logVerifyCode(String phone, String code, String provider, boolean success, String resultMsg, String bizId);
}

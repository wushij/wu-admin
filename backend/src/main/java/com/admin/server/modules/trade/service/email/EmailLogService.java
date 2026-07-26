package com.admin.server.modules.trade.service.email;

import com.admin.server.modules.trade.dal.dataobject.email.EmailLogDO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface EmailLogService extends IService<EmailLogDO> {

    void recordLog(String email, String subject, String content, String scene,
                   String provider, boolean success, String resultMsg, String ip);
}

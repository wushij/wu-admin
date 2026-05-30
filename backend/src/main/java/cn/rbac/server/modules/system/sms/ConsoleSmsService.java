package cn.rbac.server.modules.system.sms;

import cn.rbac.server.modules.system.service.sms.SmsLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsoleSmsService implements SmsService {

    private final SmsLogService smsLogService;

    @Override
    public boolean sendCode(String phone, String code) {
        return sendCodeWithTemplate(phone, code, null);
    }

    @Override
    public boolean sendCodeWithTemplate(String phone, String code, String templateCode) {
        log.info("============================================");
        log.info("【短信验证码 - 控制台模式】");
        log.info("手机号: {}", phone);
        if (templateCode != null && !templateCode.isBlank()) {
            log.info("模板: {}", templateCode);
        }
        log.info("验证码: {}", code);
        log.info("有效期: 5分钟");
        log.info("============================================");
        smsLogService.logVerifyCode(phone, code, getProviderName(), true, "控制台打印模式", null);
        return true;
    }

    @Override
    public boolean sendNotice(String phone, String title, String content) {
        log.info("【短信通知 - 控制台模式】phone={}, title={}, content={}", phone, title, content);
        return true;
    }

    @Override
    public String getProviderName() {
        return "console";
    }
}

package com.admin.server.modules.system.sms;

public interface SmsService {

    boolean sendCode(String phone, String code);

    /** 指定模板发送（测试发送等场景）；不支持的服务商可忽略 templateCode 走默认模板 */
    boolean sendCodeWithTemplate(String phone, String code, String templateCode);

    boolean sendNotice(String phone, String title, String content);

    String getProviderName();
}

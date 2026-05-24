package com.admin.server.modules.system.service.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.admin.server.modules.system.framework.cache.SysConfigCacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service
public class SystemConfigHelper {

    public static final String GROUP_SITE = "site";
    public static final String GROUP_SESSION = "session";
    public static final String GROUP_FILE = "file";
    public static final String GROUP_RATE_LIMIT = "rateLimit";
    public static final String GROUP_LOGIN = "login";
    public static final String GROUP_REGISTER = "register";
    public static final String GROUP_SECURITY = "security";
    public static final String GROUP_THIRD_PARTY = "thirdParty";
    public static final String GROUP_PAYMENT = "payment";
    public static final String GROUP_SMS = "sms";
    public static final String GROUP_EMAIL = "email";
    public static final String GROUP_AI = "ai";
    public static final String CAPTCHA_TYPE_IMAGE = "image";
    public static final String CAPTCHA_TYPE_SLIDER = "slider";
    public static final String CAPTCHA_TYPE_SMS = "sms";
    /** @deprecated 服务端已不再接受 slider_verified，须传 uuid + offsetX */
    public static final String SLIDER_VERIFIED_CODE = "slider_verified";
    public static final String LOGIN_TYPE_ACCOUNT = "account";
    public static final String LOGIN_TYPE_SMS = "sms";
    public static final String LOGIN_TYPE_EMAIL = "email";

    /** 平台级上传上限（MB），与 spring.servlet.multipart 一致，后台配置不得超过此值 */
    public static final int PLATFORM_MAX_FILE_MB = 500;

    @Resource
    private SysConfigCacheService sysConfigCacheService;

    @Value("${auth.security.captcha-enabled:true}")
    private boolean defaultCaptchaEnabled;

    @Value("${file.storage.max-size-mb:50}")
    private int defaultMaxSizeMb;

    @Value("${file.storage.allowed-extensions:jpg,jpeg,png,gif,webp}")
    private String defaultAllowedExtensions;

    @Value("${auth.security.captcha-per-ip-minute:40}")
    private int defaultCaptchaPerIpMinute;

    @Value("${auth.security.login-per-ip-minute:30}")
    private int defaultLoginPerIpMinute;

    @Value("${auth.security.register-per-ip-minute:10}")
    private int defaultRegisterPerIpMinute;

    public JSONObject getGroupJson(String groupCode) {
        return sysConfigCacheService.getGroupJson(groupCode);
    }

    // ---------- 基础信息 ----------
    public String getPlatformName() {
        return getGroupJson(GROUP_SITE).getStr("platformName", "Admin Platform");
    }

    public String getPlatformSubtitle() {
        return getGroupJson(GROUP_SITE).getStr("platformSubtitle", "统一运维 · 高效管控");
    }

    public String getLoginWelcome() {
        return getGroupJson(GROUP_SITE).getStr("loginWelcome", "Welcome");
    }

    public String getRegisterTitle() {
        return getGroupJson(GROUP_SITE).getStr("registerTitle", "Sign Up");
    }

    public String getCopyright() {
        return getGroupJson(GROUP_SITE).getStr("copyright", "");
    }

    public boolean isIcpEnabled() {
        return getGroupJson(GROUP_SITE).getBool("icpEnabled", true);
    }

    public String getIcpNumber() {
        return getGroupJson(GROUP_SITE).getStr("icpNumber", "粤ICP备XXXXXXXX号-1");
    }

    public String getIcpUrl() {
        return getGroupJson(GROUP_SITE).getStr("icpUrl", "https://beian.miit.gov.cn");
    }

    // ---------- 会话 ----------
    public int getTokenExpireHours() {
        int hours = getGroupJson(GROUP_SESSION).getInt("tokenExpireHours", 24);
        return Math.max(1, Math.min(hours, 720));
    }

    public long getTokenExpirationMs() {
        return getTokenExpireHours() * 3600_000L;
    }

    /**
     * 会话签名密钥（SM3 签名 / SM4 加密）在 Redis 中的有效期，单位：小时。
     * 默认 24 小时，与会话 Token 有效期（tokenExpireHours）保持完全一致的单位与设计。
     * 范围 1～120 小时（即 1 小时 ～ 5 天）。
     */
    public int getSessionSignExpireHours() {
        int hours = getGroupJson(GROUP_SESSION).getInt("sessionSignExpireHours", 24);
        return Math.max(1, Math.min(hours, 120));
    }

    /**
     * 会话签名密钥在 Redis 中的实际 TTL（分钟），由小时换算而来，供底层存储使用。
     */
    public long getSessionSignTtlMinutes() {
        return (long) getSessionSignExpireHours() * 60;
    }

    // ---------- 文件 ----------
    public int getFileMaxSizeMb() {
        int n = getGroupJson(GROUP_FILE).getInt("maxSizeMb", defaultMaxSizeMb);
        return n < 1 ? defaultMaxSizeMb : Math.min(n, PLATFORM_MAX_FILE_MB);
    }

    public String getFileAllowedExtensions() {
        String ext = getGroupJson(GROUP_FILE).getStr("allowedExtensions", defaultAllowedExtensions);
        return StrUtil.isBlank(ext) ? defaultAllowedExtensions : ext.trim();
    }

    // ---------- 接口限流 ----------
    public int getCaptchaPerIpMinute() {
        return clampRate(getGroupJson(GROUP_RATE_LIMIT).getInt("captchaPerIpMinute", defaultCaptchaPerIpMinute));
    }

    public int getLoginPerIpMinute() {
        return clampRate(getGroupJson(GROUP_RATE_LIMIT).getInt("loginPerIpMinute", defaultLoginPerIpMinute));
    }

    public int getRegisterPerIpMinute() {
        return clampRate(getGroupJson(GROUP_RATE_LIMIT).getInt("registerPerIpMinute", defaultRegisterPerIpMinute));
    }

    /** 短信发送：同一 IP 每分钟上限 */
    public int getSmsPerIpMinute() {
        return clampRate(getGroupJson(GROUP_RATE_LIMIT).getInt("smsPerIpMinute", 5));
    }

    /** 短信发送：同一手机号两次发送最小间隔（秒） */
    public int getSmsSendIntervalSeconds() {
        int sec = getGroupJson(GROUP_RATE_LIMIT).getInt("smsSendIntervalSeconds", 60);
        if (sec < 30) {
            return 30;
        }
        return Math.min(sec, 300);
    }

    /** 短信发送：同一手机号每日上限（0 表示不限制） */
    public int getSmsPerPhoneDaily() {
        return clampDaily(getGroupJson(GROUP_RATE_LIMIT).getInt("smsPerPhoneDaily", 10));
    }

    /** 短信发送：同一 IP 每日上限（0 表示不限制） */
    public int getSmsPerIpDaily() {
        return clampDaily(getGroupJson(GROUP_RATE_LIMIT).getInt("smsPerIpDaily", 30));
    }

    /** AI 对话：单用户每分钟请求次数上限（0 表示不限制），归属限流分组 */
    public int getAiChatPerUserMinute() {
        return clampRate(getGroupJson(GROUP_RATE_LIMIT).getInt("aiChatPerUserMinute", 8));
    }

    private int clampRate(int n) {
        if (n <= 0) {
            return 0;
        }
        return Math.min(n, 200);
    }

    private int clampDaily(int n) {
        if (n < 0) {
            return 0;
        }
        return Math.min(n, 500);
    }

    // ---------- 登录 / 注册 ----------
    public boolean isCaptchaEnabled() {
        JSONObject login = getGroupJson(GROUP_LOGIN);
        if (login.containsKey("captchaEnabled")) {
            return login.getBool("captchaEnabled", true);
        }
        return defaultCaptchaEnabled;
    }

    public String getCaptchaType() {
        JSONObject login = getGroupJson(GROUP_LOGIN);
        String type = login.getStr("captchaType", CAPTCHA_TYPE_IMAGE);
        if (StrUtil.isBlank(type) || CAPTCHA_TYPE_SMS.equals(type)) {
            return CAPTCHA_TYPE_IMAGE;
        }
        return CAPTCHA_TYPE_SLIDER.equals(type) ? CAPTCHA_TYPE_SLIDER : CAPTCHA_TYPE_IMAGE;
    }

    /** 是否开启短信验证码登录（与图形/滑块验证码独立） */
    public boolean isSmsLoginEnabled() {
        JSONObject login = getGroupJson(GROUP_LOGIN);
        if (login.containsKey("smsLoginEnabled")) {
            return login.getBool("smsLoginEnabled", false);
        }
        return CAPTCHA_TYPE_SMS.equals(login.getStr("captchaType", ""));
    }

    /** 短信登录获取验证码前是否需滑块验证 */
    public boolean isSmsLoginSliderCaptchaEnabled() {
        JSONObject login = getGroupJson(GROUP_LOGIN);
        if (!isSmsLoginEnabled()) {
            return false;
        }
        return login.getBool("smsLoginSliderCaptchaEnabled", false);
    }

    /** 是否开启邮箱验证码快捷登录 */
    public boolean isEmailLoginEnabled() {
        if (!isEmailEnabled()) {
            return false;
        }
        JSONObject login = getGroupJson(GROUP_LOGIN);
        return login.getBool("emailLoginEnabled", false);
    }

    /** 邮箱登录获取验证码前是否需滑块验证 */
    public boolean isEmailLoginSliderCaptchaEnabled() {
        JSONObject login = getGroupJson(GROUP_LOGIN);
        if (!isEmailLoginEnabled()) {
            return false;
        }
        return login.getBool("emailLoginSliderCaptchaEnabled", false);
    }

    public boolean isRememberMeEnabled() {
        return getGroupJson(GROUP_LOGIN).getBool("rememberMe", true);
    }

    public int getMaxRetryCount() {
        int n = getGroupJson(GROUP_LOGIN).getInt("maxRetryCount", 5);
        return n < 1 ? 5 : Math.min(n, 20);
    }

    /** IP 登录失败锁定阈值（可与账号分开配置，默认更宽松） */
    public int getMaxRetryCountIp() {
        int n = getGroupJson(GROUP_LOGIN).getInt("maxRetryCountIp", 20);
        return n < 1 ? 20 : Math.min(n, 50);
    }

    public int getLockTimeMinutes() {
        int n = getGroupJson(GROUP_LOGIN).getInt("lockTime", 10);
        return n < 1 ? 10 : Math.min(n, 120);
    }

    public boolean isRegisterEnabled() {
        return getGroupJson(GROUP_REGISTER).getBool("enabled", true);
    }

    public boolean isRegisterCaptchaEnabled() {
        JSONObject reg = getGroupJson(GROUP_REGISTER);
        if (reg.containsKey("captchaEnabled")) {
            return reg.getBool("captchaEnabled", true);
        }
        return isCaptchaEnabled();
    }

    public String getRegisterCaptchaType() {
        JSONObject reg = getGroupJson(GROUP_REGISTER);
        if (reg.containsKey("captchaType")) {
            String type = reg.getStr("captchaType", CAPTCHA_TYPE_IMAGE);
            return StrUtil.isBlank(type) ? CAPTCHA_TYPE_IMAGE : type;
        }
        return getCaptchaType();
    }

    public String getRegisterDefaultRoleCode() {
        String code = getGroupJson(GROUP_REGISTER).getStr("defaultRoleCode", "user");
        return StrUtil.isBlank(code) ? "user" : code.trim();
    }

    public boolean isRegisterNeedAudit() {
        return getGroupJson(GROUP_REGISTER).getBool("needAudit", false);
    }

    /** 注册审核通知人；为空时由业务层回退至超级管理员 */
    public List<Long> getRegisterAuditorUserIds() {
        JSONArray arr = getGroupJson(GROUP_REGISTER).getJSONArray("auditorUserIds");
        if (arr == null || arr.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (Object o : arr) {
            if (o == null) {
                continue;
            }
            long id = o instanceof Number ? ((Number) o).longValue() : Long.parseLong(String.valueOf(o));
            if (id > 0 && !ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    public int getRegisterMinPasswordLength() {
        int n = getGroupJson(GROUP_REGISTER).getInt("minPasswordLength", 6);
        return n < 6 ? 6 : Math.min(n, 32);
    }

    // ---------- 前端与 API 安全 ----------
    public boolean isDisableDevtool() {
        return getGroupJson(GROUP_SECURITY).getBool("disableDevtool", true);
    }

    /** Sa-Token is-concurrent，默认 false（禁止多端同时在线） */
    public boolean isConcurrentLogin() {
        return getGroupJson(GROUP_SECURITY).getBool("isConcurrent", false);
    }

    /** SM4 数据加密开启状态 */
    public boolean isSm4EncryptEnabled() {
        return getGroupJson(GROUP_SECURITY).getBool("sm4EncryptEnabled", false);
    }

    /**
     * SM4 数据加密是否生效。
     *
     * 注意：自从引入 session-sign-init 会话随机密钥机制后，SM4 密钥由后端随机生成并存入 Redis，
     * 不再依赖 DB 中配置的静态 sm4SecretKey。只要管理员将加密开关打开即生效。
     */
    public boolean isSm4EncryptEffective() {
        return isSm4EncryptEnabled();
    }

    /** SM2 数字签名开启状态 */
    public boolean isSm2SignEnabled() {
        return getGroupJson(GROUP_SECURITY).getBool("sm2SignEnabled", false);
    }

    /** 国密 HMAC-SM3 签名 Key (未配置时优先回退使用 SM4 对称密钥) */
    public String getSm3SignKey() {
        String key = getGroupJson(GROUP_SECURITY).getStr("sm3SignKey", "");
        if (StrUtil.isNotBlank(key)) return key.trim();
        return getSm4SecretKey();
    }

    /**
     * SM2/HMAC-SM3 数字签名是否生效。
     *
     * 注意：自从引入 session-sign-init 会话随机密钥机制后，签名密钥由后端随机生成并存入 Redis，
     * 不再依赖 DB 中配置的静态 sm3SignKey / sm4SecretKey。
     * 因此，只要管理员在后台将签名开关打开，签名功能即生效，无需额外配置静态密钥字段。
     */
    public boolean isSm2SignEffective() {
        return isSm2SignEnabled() || getGroupJson(GROUP_SECURITY).getBool("sm3SignEnabled", false);
    }

    /** 时间戳校验开启状态（默认开启） */
    public boolean isTimestampEnabled() {
        return getGroupJson(GROUP_SECURITY).getBool("timestampEnabled", true);
    }

    /** Nonce 随机数防重放校验开启状态（默认开启） */
    public boolean isNonceEnabled() {
        return getGroupJson(GROUP_SECURITY).getBool("nonceEnabled", true);
    }

    /** SM4 对称秘钥（16 字节密钥），未配置时返回空串（不提供硬编码兜底密钥） */
    public String getSm4SecretKey() {
        String key = getGroupJson(GROUP_SECURITY).getStr("sm4SecretKey", "");
        return StrUtil.isBlank(key) ? "" : key.trim();
    }

    // ---------- 短信配置 ----------
    public boolean isSmsEnabled() {
        return getGroupJson(GROUP_SMS).getBool("enabled", false);
    }

    public String getSmsProvider() {
        String provider = getGroupJson(GROUP_SMS).getStr("provider", "aliyunAuth");
        if (StrUtil.isBlank(provider)) {
            return "aliyunAuth";
        }
        provider = provider.trim();
        // 旧值 aliyun 已切换为短信认证方案
        if ("aliyun".equals(provider)) {
            return "aliyunAuth";
        }
        return provider;
    }

    public String getSmsAccessKeyId() {
        return getGroupJson(GROUP_SMS).getStr("accessKeyId", "");
    }

    public String getSmsAccessKeySecret() {
        return getGroupJson(GROUP_SMS).getStr("accessKeySecret", "");
    }

    public String getSmsSignName() {
        return getGroupJson(GROUP_SMS).getStr("signName", "");
    }

    public String getSmsTencentAppId() {
        return getGroupJson(GROUP_SMS).getStr("tencentAppId", "");
    }

    public String getSmsTemplateVerifyCode() {
        return getGroupJson(GROUP_SMS).getStr("templateVerifyCode", "");
    }

    public String getSmsTemplateResetPassword() {
        return getGroupJson(GROUP_SMS).getStr("templateResetPassword", "");
    }

    public String getSmsTemplateModifyPhone() {
        return getGroupJson(GROUP_SMS).getStr("templateModifyPhone", "");
    }

    public String getSmsTemplateBindPhone() {
        return getGroupJson(GROUP_SMS).getStr("templateBindPhone", "");
    }

    public String getSmsTemplateVerifyBindPhone() {
        return getGroupJson(GROUP_SMS).getStr("templateVerifyBindPhone", "");
    }

    /** @deprecated 短信认证不支持通知模板，保留读取兼容旧 JSON */
    public String getSmsTemplateNotice() {
        return getGroupJson(GROUP_SMS).getStr("templateNotice", "");
    }

    /** 短信认证方案名称（CheckSmsVerifyCode），可为空 */
    public String getSmsSchemeName() {
        return getGroupJson(GROUP_SMS).getStr("schemeName", "");
    }

    /** 验证码有效期（分钟），用于短信认证模板参数 min */
    public int getSmsCodeExpireMinutes() {
        int minutes = getGroupJson(GROUP_SMS).getInt("codeExpireMinutes", 5);
        return minutes < 1 ? 5 : Math.min(minutes, 30);
    }

    public boolean isAliyunAuthSmsProvider() {
        return "aliyunAuth".equals(getSmsProvider());
    }

    // ---------- 邮件配置 ----------
    public boolean isEmailEnabled() {
        return getGroupJson(GROUP_EMAIL).getBool("enabled", true);
    }

    public String getEmailProvider() {
        return getGroupJson(GROUP_EMAIL).getStr("provider", "qq");
    }

    public String getEmailHost() {
        return getGroupJson(GROUP_EMAIL).getStr("host", "smtp.qq.com");
    }

    public int getEmailPort() {
        return getGroupJson(GROUP_EMAIL).getInt("port", 465);
    }

    public String getEmailUsername() {
        return getGroupJson(GROUP_EMAIL).getStr("username", "");
    }

    public String getEmailPassword() {
        return getGroupJson(GROUP_EMAIL).getStr("password", "");
    }

    public String getEmailFromName() {
        return getGroupJson(GROUP_EMAIL).getStr("fromName", "wu-admin 系统团队");
    }

    public boolean isEmailAuthEnabled() {
        return getGroupJson(GROUP_EMAIL).getBool("authEnabled", true);
    }

    public String getEmailSecurityType() {
        return getGroupJson(GROUP_EMAIL).getStr("securityType", "SSL");
    }

    public int getEmailConnectionTimeoutMs() {
        int ms = getGroupJson(GROUP_EMAIL).getInt("connectionTimeoutMs", 5000);
        return ms < 1000 ? 5000 : Math.min(ms, 30000);
    }

    public int getEmailTimeoutMs() {
        int ms = getGroupJson(GROUP_EMAIL).getInt("timeoutMs", 5000);
        return ms < 1000 ? 5000 : Math.min(ms, 30000);
    }

    public int getEmailWriteTimeoutMs() {
        int ms = getGroupJson(GROUP_EMAIL).getInt("writeTimeoutMs", 5000);
        return ms < 1000 ? 5000 : Math.min(ms, 30000);
    }

    public String getEmailEncoding() {
        String enc = getGroupJson(GROUP_EMAIL).getStr("encoding", "UTF-8");
        return StrUtil.isBlank(enc) ? "UTF-8" : enc.trim();
    }

    public boolean isEmailDebug() {
        return getGroupJson(GROUP_EMAIL).getBool("debug", false);
    }

    public int getEmailCodeExpireMinutes() {
        int m = getGroupJson(GROUP_EMAIL).getInt("codeExpireMinutes", 5);
        return m < 1 ? 5 : Math.min(m, 30);
    }

    public int getEmailCodeLength() {
        int len = getGroupJson(GROUP_EMAIL).getInt("codeLength", 6);
        return len < 4 ? 6 : Math.min(len, 8);
    }

    public int getEmailDailyLimitPerEmail() {
        int limit = getGroupJson(GROUP_EMAIL).getInt("dailyLimitPerEmail", 20);
        return limit < 1 ? 20 : Math.min(limit, 100);
    }

    public int getEmailSendIntervalSeconds() {
        int s = getGroupJson(GROUP_EMAIL).getInt("sendIntervalSeconds", 60);
        return s < 10 ? 60 : Math.min(s, 300);
    }

    public JavaMailSenderImpl buildDynamicMailSender() {
        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setHost(getEmailHost());
        impl.setPort(getEmailPort());
        impl.setUsername(getEmailUsername());
        impl.setPassword(getEmailPassword());
        impl.setDefaultEncoding(getEmailEncoding());

        Properties props = impl.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(isEmailAuthEnabled()));

        String secType = getEmailSecurityType();
        if ("SSL".equalsIgnoreCase(secType)) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", String.valueOf(getEmailPort()));
        } else if ("TLS".equalsIgnoreCase(secType) || "STARTTLS".equalsIgnoreCase(secType)) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        int connTimeout = getEmailConnectionTimeoutMs();
        int timeout = getEmailTimeoutMs();
        int writeTimeout = getEmailWriteTimeoutMs();

        props.put("mail.smtp.connectiontimeout", String.valueOf(connTimeout));
        props.put("mail.smtp.timeout", String.valueOf(timeout));
        props.put("mail.smtp.writetimeout", String.valueOf(writeTimeout));

        if (isEmailDebug()) {
            props.put("mail.debug", "true");
        }

        return impl;
    }

    /** 工作台统计：每组配置只读一次 Redis，避免重复 getGroupJson */
    public void fillDashboardConfigFields(Map<String, Object> stats) {
        JSONObject site = getGroupJson(GROUP_SITE);
        stats.put("platformName", site.getStr("platformName", "Admin Platform"));
        stats.put("platformSubtitle", site.getStr("platformSubtitle", "统一运维 · 高效管控"));

        JSONObject file = getGroupJson(GROUP_FILE);
        int maxMb = file.getInt("maxSizeMb", defaultMaxSizeMb);
        stats.put("fileMaxSizeMb", maxMb < 1 ? defaultMaxSizeMb : Math.min(maxMb, PLATFORM_MAX_FILE_MB));
        String ext = file.getStr("allowedExtensions", defaultAllowedExtensions);
        stats.put("fileAllowedExtensions", StrUtil.isBlank(ext) ? defaultAllowedExtensions : ext.trim());

        JSONObject session = getGroupJson(GROUP_SESSION);
        int hours = session.getInt("tokenExpireHours", 24);
        stats.put("tokenExpireHours", Math.max(1, Math.min(hours, 720)));

        JSONObject login = getGroupJson(GROUP_LOGIN);
        stats.put("loginCaptchaEnabled", isCaptchaEnabled());
        stats.put("loginCaptchaType", getCaptchaType());
        stats.put("loginRememberMe", login.getBool("rememberMe", true));
        int maxRetry = login.getInt("maxRetryCount", 5);
        stats.put("loginMaxRetryCount", maxRetry < 1 ? 5 : Math.min(maxRetry, 20));
        int lockMin = login.getInt("lockTime", 10);
        stats.put("loginLockTimeMinutes", lockMin < 1 ? 10 : Math.min(lockMin, 120));

        JSONObject register = getGroupJson(GROUP_REGISTER);
        stats.put("registerEnabled", register.getBool("enabled", true));
        stats.put("registerNeedAudit", register.getBool("needAudit", false));
    }

    public Map<String, Object> buildPublicConfig() {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> site = new HashMap<>();
        site.put("platformName", getPlatformName());
        site.put("platformSubtitle", getPlatformSubtitle());
        site.put("loginWelcome", getLoginWelcome());
        site.put("registerTitle", getRegisterTitle());
        site.put("copyright", getCopyright());
        site.put("icpEnabled", isIcpEnabled());
        site.put("icpNumber", getIcpNumber());
        site.put("icpUrl", getIcpUrl());
        result.put("site", site);

        JSONObject loginJson = getGroupJson(GROUP_LOGIN);
        Map<String, Object> login = new HashMap<>();
        login.put("captchaEnabled", isCaptchaEnabled());
        login.put("captchaType", getCaptchaType());
        login.put("smsLoginEnabled", isSmsLoginEnabled());
        login.put("smsLoginSliderCaptchaEnabled", isSmsLoginSliderCaptchaEnabled());
        login.put("emailLoginEnabled", isEmailLoginEnabled());
        login.put("emailLoginSliderCaptchaEnabled", isEmailLoginSliderCaptchaEnabled());
        login.put("rememberMe", loginJson.getBool("rememberMe", true));
        login.put("maxRetryCount", getMaxRetryCount());
        login.put("maxRetryCountIp", getMaxRetryCountIp());
        login.put("lockTime", getLockTimeMinutes());
        login.put("smsEnabled", isSmsEnabled());
        login.put("emailEnabled", isEmailEnabled());
        result.put("login", login);

        Map<String, Object> register = new HashMap<>();
        register.put("enabled", isRegisterEnabled());
        register.put("captchaEnabled", isRegisterCaptchaEnabled());
        register.put("captchaType", getRegisterCaptchaType());
        register.put("defaultRoleCode", getRegisterDefaultRoleCode());
        register.put("needAudit", isRegisterNeedAudit());
        register.put("minPasswordLength", getRegisterMinPasswordLength());
        result.put("register", register);

        Map<String, Object> security = new HashMap<>();
        security.put("disableDevtool", isDisableDevtool());

        // 【安全加固 P0】接口签名/加密密钥不再通过公开接口明文下发给客户端。
        // 客户端须调用 POST /auth/session-sign-init（携带 clientId）换取一次性会话临时密钥。
        // 此处仅告知客户端「服务端是否已启用签名/加密」，不暴露任何密钥材料。
        boolean sm3SignActive = isSm2SignEffective();
        boolean sm4Active = isSm4EncryptEffective();
        security.put("sm3SignEnabled", sm3SignActive);
        security.put("sm2SignEnabled", sm3SignActive);
        security.put("sm4EncryptEnabled", sm4Active);
        // 严禁输出：sm4Key / sm3SignKey 等任何密钥材料

        result.put("security", security);

        Map<String, Object> ai = new HashMap<>();
        ai.put("assistantEnabled", isAiAssistantEnabled());
        result.put("ai", ai);

        return result;
    }

    // ---------- AI 助手 ----------

    /** AI 助手悬浮小窗/全局 AI 功能开关（true 开启，false 关闭） */
    public boolean isAiAssistantEnabled() {
        return getGroupJson(GROUP_AI).getBool("assistantEnabled", true);
    }

    /** AI 助手全局项目知识块（Markdown，注入 system 提示词），未配置时返回空串 */
    public String getAiGlobalKnowledge() {
        return getGroupJson(GROUP_AI).getStr("globalKnowledge", "");
    }

    /** AI 助手回答边界策略：focus(聚焦本系统，默认) / open(开放问答) */
    public String getAiAnswerScope() {
        String scope = getGroupJson(GROUP_AI).getStr("answerScope", "focus");
        return "open".equalsIgnoreCase(scope) ? "open" : "focus";
    }

    /** AI 对话：单用户每日 token 兜底配额（0 表示不限制），未命中角色规则时生效 */
    public long getAiTokensPerUserDaily() {
        return clampTokenQuota(getGroupJson(GROUP_AI).getLong("tokensPerUserDaily", 100000L));
    }

    /**
     * AI 对话：角色级每日 token 配额映射 roleId -&gt; tokensDaily（已 clamp）。
     * 配置缺失或解析异常时返回空映射，由调用方回退兜底配额。
     */
    public Map<Long, Long> getAiRoleTokenQuotas() {
        Map<Long, Long> result = new HashMap<>();
        try {
            JSONArray arr = getGroupJson(GROUP_AI).getJSONArray("roleTokenQuotas");
            if (arr == null) {
                return result;
            }
            for (int i = 0; i < arr.size(); i++) {
                JSONObject item = arr.getJSONObject(i);
                if (item == null) {
                    continue;
                }
                Long roleId = item.getLong("roleId");
                if (roleId == null || roleId <= 0) {
                    continue;
                }
                result.put(roleId, clampTokenQuota(item.getLong("tokensDaily", 0L)));
            }
        } catch (Exception e) {
            return new HashMap<>();
        }
        return result;
    }

    /** token 配额收敛：非正数归零（表示不限制），上限 1000 万 */
    private long clampTokenQuota(long n) {
        if (n <= 0) {
            return 0L;
        }
        return Math.min(n, 10_000_000L);
    }
}

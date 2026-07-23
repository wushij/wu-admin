package com.admin.server.modules.system.service.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.admin.server.modules.system.framework.cache.SysConfigCacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
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
    public static final String CAPTCHA_TYPE_IMAGE = "image";
    public static final String CAPTCHA_TYPE_SLIDER = "slider";
    public static final String CAPTCHA_TYPE_SMS = "sms";
    /** @deprecated 服务端已不再接受 slider_verified，须传 uuid + offsetX */
    public static final String SLIDER_VERIFIED_CODE = "slider_verified";
    public static final String LOGIN_TYPE_ACCOUNT = "account";
    public static final String LOGIN_TYPE_SMS = "sms";

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

    // ---------- 会话 ----------
    public int getTokenExpireHours() {
        int hours = getGroupJson(GROUP_SESSION).getInt("tokenExpireHours", 24);
        return Math.max(1, Math.min(hours, 720));
    }

    public long getTokenExpirationMs() {
        return getTokenExpireHours() * 3600_000L;
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

    // ---------- 前端安全 ----------
    public boolean isDisableDevtool() {
        return getGroupJson(GROUP_SECURITY).getBool("disableDevtool", false);
    }

    /** Sa-Token is-concurrent，默认 false（禁止多端同时在线） */
    public boolean isConcurrentLogin() {
        return getGroupJson(GROUP_SECURITY).getBool("isConcurrent", false);
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
        result.put("site", site);

        JSONObject loginJson = getGroupJson(GROUP_LOGIN);
        Map<String, Object> login = new HashMap<>();
        login.put("captchaEnabled", isCaptchaEnabled());
        login.put("captchaType", getCaptchaType());
        login.put("smsLoginEnabled", isSmsLoginEnabled());
        login.put("smsLoginSliderCaptchaEnabled", isSmsLoginSliderCaptchaEnabled());
        login.put("rememberMe", loginJson.getBool("rememberMe", true));
        login.put("maxRetryCount", getMaxRetryCount());
        login.put("maxRetryCountIp", getMaxRetryCountIp());
        login.put("lockTime", getLockTimeMinutes());
        login.put("smsEnabled", isSmsEnabled());
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
        result.put("security", security);

        return result;
    }
}

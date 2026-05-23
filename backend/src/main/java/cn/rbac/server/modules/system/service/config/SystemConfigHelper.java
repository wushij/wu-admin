package cn.rbac.server.modules.system.service.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.rbac.server.modules.system.framework.cache.SysConfigCacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Component
public class SystemConfigHelper {

    public static final String GROUP_SITE = "site";
    public static final String GROUP_SESSION = "session";
    public static final String GROUP_FILE = "file";
    public static final String GROUP_RATE_LIMIT = "rateLimit";
    public static final String GROUP_LOGIN = "login";
    public static final String GROUP_REGISTER = "register";
    public static final String CAPTCHA_TYPE_IMAGE = "image";
    public static final String CAPTCHA_TYPE_SLIDER = "slider";
    public static final String SLIDER_VERIFIED_CODE = "slider_verified";

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

    private int clampRate(int n) {
        if (n <= 0) {
            return 0;
        }
        return Math.min(n, 200);
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
        return StrUtil.isBlank(type) ? CAPTCHA_TYPE_IMAGE : type;
    }

    public boolean isRememberMeEnabled() {
        return getGroupJson(GROUP_LOGIN).getBool("rememberMe", true);
    }

    public int getMaxRetryCount() {
        int n = getGroupJson(GROUP_LOGIN).getInt("maxRetryCount", 5);
        return n < 1 ? 5 : Math.min(n, 20);
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

    public String getRegisterDefaultRoleCode() {
        String code = getGroupJson(GROUP_REGISTER).getStr("defaultRoleCode", "user");
        return StrUtil.isBlank(code) ? "user" : code.trim();
    }

    public boolean isRegisterNeedAudit() {
        return getGroupJson(GROUP_REGISTER).getBool("needAudit", false);
    }

    public int getRegisterMinPasswordLength() {
        int n = getGroupJson(GROUP_REGISTER).getInt("minPasswordLength", 6);
        return n < 6 ? 6 : Math.min(n, 32);
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
        login.put("rememberMe", loginJson.getBool("rememberMe", true));
        login.put("maxRetryCount", getMaxRetryCount());
        login.put("lockTime", getLockTimeMinutes());
        result.put("login", login);

        Map<String, Object> register = new HashMap<>();
        register.put("enabled", isRegisterEnabled());
        register.put("captchaEnabled", isRegisterCaptchaEnabled());
        register.put("defaultRoleCode", getRegisterDefaultRoleCode());
        register.put("needAudit", isRegisterNeedAudit());
        register.put("minPasswordLength", getRegisterMinPasswordLength());
        result.put("register", register);

        return result;
    }
}

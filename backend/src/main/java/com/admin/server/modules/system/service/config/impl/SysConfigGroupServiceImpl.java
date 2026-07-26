package com.admin.server.modules.system.service.config.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.system.dal.dataobject.config.SysConfigGroupDO;
import com.admin.server.modules.system.dal.mysql.config.SysConfigGroupMapper;
import com.admin.server.modules.system.framework.cache.SysConfigCacheService;
import com.admin.server.modules.system.service.config.SysConfigGroupService;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Set;

import com.admin.server.modules.infra.framework.operlog.OperLogContext;

@Service
public class SysConfigGroupServiceImpl implements SysConfigGroupService {

    private static final Set<String> LOGIN_CAPTCHA_TYPES = Set.of(
            SystemConfigHelper.CAPTCHA_TYPE_IMAGE,
            SystemConfigHelper.CAPTCHA_TYPE_SLIDER);

    private static final Set<String> REGISTER_CAPTCHA_TYPES = Set.of(
            SystemConfigHelper.CAPTCHA_TYPE_IMAGE,
            SystemConfigHelper.CAPTCHA_TYPE_SLIDER);

    @Resource
    private SysConfigGroupMapper configGroupMapper;
    @Resource
    private SysConfigCacheService sysConfigCacheService;
    @Resource
    private SystemConfigHelper systemConfigHelper;

    @Override
    public List<SysConfigGroupDO> listAll() {
        return configGroupMapper.selectList(
                new LambdaQueryWrapper<SysConfigGroupDO>().orderByAsc(SysConfigGroupDO::getId));
    }

    @Override
    public SysConfigGroupDO getByGroupCode(String groupCode) {
        return configGroupMapper.selectOne(
                new LambdaQueryWrapper<SysConfigGroupDO>().eq(SysConfigGroupDO::getGroupCode, groupCode));
    }

    @Override
    public void updateConfig(String groupCode, String configValue) {
        if (!JSONUtil.isTypeJSON(configValue)) {
            throw new BusinessException("配置内容必须是合法 JSON");
        }
        JSONObject json = JSONUtil.parseObj(configValue);
        validateGroupConfig(groupCode, json);
        SysConfigGroupDO row = getByGroupCode(groupCode);
        if (row == null) {
            throw new BusinessException("配置分组不存在: " + groupCode);
        }

        JSONObject oldJson = null;
        if (StrUtil.isNotBlank(row.getConfigValue()) && JSONUtil.isTypeJSON(row.getConfigValue())) {
            oldJson = JSONUtil.parseObj(row.getConfigValue());
        } else {
            oldJson = new JSONObject();
        }

        List<String> diffItems = buildConfigDiffs(groupCode, oldJson, json);
        if (diffItems.isEmpty()) {
            // 未发生任何实际配置改动：跳过生成无意义的操作日志与 DB 更新
            OperLogContext.setSkipLog(true);
            return;
        }

        String groupTitle = getGroupTitle(groupCode);
        OperLogContext.setTitle("系统配置 - " + groupTitle);
        OperLogContext.setDiffItems(diffItems);
        OperLogContext.setAction("修改系统配置「" + groupTitle + "」: " + String.join("；", diffItems));

        row.setConfigValue(json.toString());
        configGroupMapper.updateById(row);
        sysConfigCacheService.refreshAll();
    }

    private String getGroupTitle(String groupCode) {
        if (groupCode == null) return "未知分组";
        switch (groupCode) {
            case SystemConfigHelper.GROUP_SITE: return "基础信息";
            case SystemConfigHelper.GROUP_SESSION: return "会话令牌";
            case SystemConfigHelper.GROUP_FILE: return "文件存储";
            case SystemConfigHelper.GROUP_RATE_LIMIT: return "接口限流";
            case SystemConfigHelper.GROUP_LOGIN: return "登录认证";
            case SystemConfigHelper.GROUP_REGISTER: return "注册认证";
            case SystemConfigHelper.GROUP_SMS: return "短信配置";
            case SystemConfigHelper.GROUP_EMAIL: return "邮件配置";
            case SystemConfigHelper.GROUP_THIRD_PARTY: return "第三方配置";
            case SystemConfigHelper.GROUP_PAYMENT: return "支付配置";
            case SystemConfigHelper.GROUP_SECURITY: return "安全防刷";
            default: return groupCode;
        }
    }

    private List<String> buildConfigDiffs(String groupCode, JSONObject oldJson, JSONObject newJson) {
        List<String> diffs = new java.util.ArrayList<>();
        if (oldJson == null) oldJson = new JSONObject();
        if (newJson == null) newJson = new JSONObject();

        Set<String> allKeys = new java.util.LinkedHashSet<>();
        allKeys.addAll(oldJson.keySet());
        allKeys.addAll(newJson.keySet());

        for (String key : allKeys) {
            Object oldVal = oldJson.get(key);
            Object newVal = newJson.get(key);
            if (java.util.Objects.equals(oldVal, newVal)) {
                continue;
            }
            String label = getFieldLabel(groupCode, key);
            String oldStr = formatConfigValue(oldVal);
            String newStr = formatConfigValue(newVal);
            diffs.add(label + ": " + oldStr + " -> " + newStr);
        }
        return diffs;
    }

    private String getFieldLabel(String groupCode, String key) {
        if (SystemConfigHelper.GROUP_LOGIN.equals(groupCode)) {
            switch (key) {
                case "captchaEnabled": return "登录人机校检";
                case "captchaType": return "验证码类型";
                case "smsLoginEnabled": return "短信验证码登录";
                case "smsLoginSliderCaptchaEnabled": return "短信发送前滑块";
                case "emailLoginEnabled": return "邮箱验证码登录";
                case "emailLoginSliderCaptchaEnabled": return "邮箱发送前滑块";
                case "rememberMe": return "记住我";
                case "maxRetryCount": return "账号最大重试";
                case "maxRetryCountIp": return "IP最大重试";
                case "lockTime": return "锁定时长";
                default: break;
            }
        } else if (SystemConfigHelper.GROUP_SITE.equals(groupCode)) {
            switch (key) {
                case "platformName": return "系统名称";
                case "platformSubtitle": return "系统副标题";
                case "loginWelcome": return "登录页欢迎语";
                case "registerTitle": return "注册页标题";
                case "copyright": return "版权信息";
                default: break;
            }
        } else if (SystemConfigHelper.GROUP_REGISTER.equals(groupCode)) {
            switch (key) {
                case "enabled": return "开放注册";
                case "captchaEnabled": return "注册验证码";
                case "captchaType": return "验证码类型";
                case "needAudit": return "注册审核";
                default: break;
            }
        } else if (SystemConfigHelper.GROUP_SMS.equals(groupCode)) {
            switch (key) {
                case "enabled": return "启用短信";
                case "provider": return "短信服务商";
                default: break;
            }
        } else if (SystemConfigHelper.GROUP_EMAIL.equals(groupCode)) {
            switch (key) {
                case "enabled": return "启用邮件";
                case "fromEmail": return "发件人邮箱";
                case "host": return "SMTP 服务器";
                default: break;
            }
        } else if (SystemConfigHelper.GROUP_SECURITY.equals(groupCode)) {
            switch (key) {
                case "disableDevtool": return "禁用开发者工具";
                case "isConcurrent": return "并发登录";
                default: break;
            }
        }
        return key;
    }

    private String formatConfigValue(Object val) {
        if (val == null) return "无";
        if (val instanceof Boolean) {
            return ((Boolean) val) ? "开启" : "关闭";
        }
        if ("image".equals(val)) return "图片";
        if ("slider".equals(val)) return "滑块";
        String s = val.toString();
        if (s.length() > 20) {
            return s.substring(0, 17) + "...";
        }
        return s;
    }

    private void validateGroupConfig(String groupCode, JSONObject json) {
        switch (groupCode) {
            case SystemConfigHelper.GROUP_SITE:
                validateSiteConfig(json);
                break;
            case SystemConfigHelper.GROUP_SESSION:
                validateSessionConfig(json);
                break;
            case SystemConfigHelper.GROUP_FILE:
                validateFileConfig(json);
                break;
            case SystemConfigHelper.GROUP_RATE_LIMIT:
                validateRateLimitConfig(json);
                break;
            case SystemConfigHelper.GROUP_LOGIN:
                validateLoginConfig(json);
                break;
            case SystemConfigHelper.GROUP_REGISTER:
                validateRegisterConfig(json);
                break;
            case SystemConfigHelper.GROUP_SMS:
                validateSmsConfig(json);
                break;
            default:
                break;
        }
    }

    private void validateSiteConfig(JSONObject json) {
        if (StrUtil.isBlank(json.getStr("platformName"))) {
            throw new BusinessException("平台名称不能为空");
        }
    }

    private void validateSessionConfig(JSONObject json) {
        int hours = json.getInt("tokenExpireHours", 24);
        if (hours < 1 || hours > 720) {
            throw new BusinessException("Token 有效期须在 1～720 小时之间");
        }
    }

    private void validateFileConfig(JSONObject json) {
        int mb = json.getInt("maxSizeMb", 50);
        if (mb < 1 || mb > SystemConfigHelper.PLATFORM_MAX_FILE_MB) {
            throw new BusinessException(
                    "文件大小上限须在 1～" + SystemConfigHelper.PLATFORM_MAX_FILE_MB + " MB 之间");
        }
        if (StrUtil.isBlank(json.getStr("allowedExtensions"))) {
            throw new BusinessException("允许扩展名不能为空");
        }
    }

    private void validateRateLimitConfig(JSONObject json) {
        validateRate(json.getInt("captchaPerIpMinute", 40), "验证码接口");
        validateRate(json.getInt("loginPerIpMinute", 30), "登录接口");
        validateRate(json.getInt("registerPerIpMinute", 10), "注册接口");
        validateRate(json.getInt("smsPerIpMinute", 5), "短信发送");
        int interval = json.getInt("smsSendIntervalSeconds", 60);
        if (interval < 30 || interval > 300) {
            throw new BusinessException("短信发送间隔须在 30～300 秒之间");
        }
        validateDailyLimit(json.getInt("smsPerPhoneDaily", 10), "手机号每日短信");
        validateDailyLimit(json.getInt("smsPerIpDaily", 30), "IP 每日短信");
    }

    private void validateDailyLimit(int n, String label) {
        if (n < 0 || n > 500) {
            throw new BusinessException(label + "上限须在 0～500 之间（0 表示不限制）");
        }
    }

    private void validateRate(int n, String label) {
        if (n < 0 || n > 200) {
            throw new BusinessException(label + "每分钟限流须在 0～200 之间（0 表示不限制）");
        }
    }

    private void validateLoginConfig(JSONObject json) {
        boolean captchaEnabled = json.getBool("captchaEnabled", true);
        if (captchaEnabled) {
            String type = json.getStr("captchaType", SystemConfigHelper.CAPTCHA_TYPE_IMAGE);
            if (SystemConfigHelper.CAPTCHA_TYPE_SMS.equals(type)) {
                throw new BusinessException("短信登录请使用「短信验证码登录」开关，验证码类型仅支持 image 或 slider");
            }
            if (StrUtil.isBlank(type) || !LOGIN_CAPTCHA_TYPES.contains(type)) {
                throw new BusinessException("验证码类型仅支持 image 或 slider");
            }
        }
        boolean smsLoginEnabled = json.getBool("smsLoginEnabled", false);
        if (smsLoginEnabled && !systemConfigHelper.isSmsEnabled()) {
            throw new BusinessException("启用短信验证码登录须先在短信配置中开启短信功能");
        }
        boolean smsLoginSliderCaptchaEnabled = json.getBool("smsLoginSliderCaptchaEnabled", false);
        if (smsLoginSliderCaptchaEnabled && !smsLoginEnabled) {
            throw new BusinessException("启用短信发送前滑块验证须先开启短信验证码登录");
        }
        int maxRetry = json.getInt("maxRetryCount", 5);
        if (maxRetry < 1 || maxRetry > 20) {
            throw new BusinessException("账号最大重试次数须在 1～20 之间");
        }
        int maxRetryIp = json.getInt("maxRetryCountIp", 20);
        if (maxRetryIp < 1 || maxRetryIp > 50) {
            throw new BusinessException("IP 最大重试次数须在 1～50 之间");
        }
        int lockTime = json.getInt("lockTime", 10);
        if (lockTime < 1 || lockTime > 120) {
            throw new BusinessException("锁定时长须在 1～120 分钟之间");
        }
    }

    private void validateRegisterConfig(JSONObject json) {
        boolean captchaEnabled = json.getBool("captchaEnabled", true);
        if (captchaEnabled) {
            String type = json.getStr("captchaType", SystemConfigHelper.CAPTCHA_TYPE_IMAGE);
            if (StrUtil.isBlank(type) || !REGISTER_CAPTCHA_TYPES.contains(type)) {
                throw new BusinessException("注册验证码类型仅支持 image 或 slider");
            }
        }
        if (json.containsKey("defaultRoleCode")) {
            String code = json.getStr("defaultRoleCode");
            if (StrUtil.isBlank(code)) {
                throw new BusinessException("默认角色编码不能为空");
            }
        }
        int minLen = json.getInt("minPasswordLength", 6);
        if (minLen < 6 || minLen > 32) {
            throw new BusinessException("密码最小长度须在 6～32 之间");
        }
    }

    private void validateSmsConfig(JSONObject json) {
        String provider = json.getStr("provider", "aliyunAuth");
        if ("aliyun".equals(provider)) {
            json.set("provider", "aliyunAuth");
            provider = "aliyunAuth";
        }
        if (StrUtil.isBlank(provider) || (!"aliyunAuth".equals(provider) && !"tencent".equals(provider))) {
            throw new BusinessException("短信服务商仅支持 aliyunAuth 或 tencent");
        }
    }
}

package cn.rbac.server.modules.system.service.config.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.rbac.server.modules.system.dal.dataobject.config.SysConfigGroupDO;
import cn.rbac.server.modules.system.dal.mysql.config.SysConfigGroupMapper;
import cn.rbac.server.modules.system.service.config.SysConfigGroupService;
import cn.rbac.server.modules.system.service.config.SystemConfigHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

@Service
public class SysConfigGroupServiceImpl implements SysConfigGroupService {

    private static final Set<String> LOGIN_CAPTCHA_TYPES = Set.of(
            SystemConfigHelper.CAPTCHA_TYPE_IMAGE,
            SystemConfigHelper.CAPTCHA_TYPE_SLIDER);

    @Resource
    private SysConfigGroupMapper configGroupMapper;

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
            throw new IllegalArgumentException("配置内容必须是合法 JSON");
        }
        JSONObject json = JSONUtil.parseObj(configValue);
        validateGroupConfig(groupCode, json);
        SysConfigGroupDO row = getByGroupCode(groupCode);
        if (row == null) {
            throw new IllegalArgumentException("配置分组不存在: " + groupCode);
        }
        row.setConfigValue(json.toString());
        configGroupMapper.updateById(row);
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
            default:
                break;
        }
    }

    private void validateSiteConfig(JSONObject json) {
        if (StrUtil.isBlank(json.getStr("platformName"))) {
            throw new IllegalArgumentException("平台名称不能为空");
        }
    }

    private void validateSessionConfig(JSONObject json) {
        int hours = json.getInt("tokenExpireHours", 24);
        if (hours < 1 || hours > 720) {
            throw new IllegalArgumentException("Token 有效期须在 1～720 小时之间");
        }
    }

    private void validateFileConfig(JSONObject json) {
        int mb = json.getInt("maxSizeMb", 50);
        if (mb < 1 || mb > SystemConfigHelper.PLATFORM_MAX_FILE_MB) {
            throw new IllegalArgumentException(
                    "文件大小上限须在 1～" + SystemConfigHelper.PLATFORM_MAX_FILE_MB + " MB 之间");
        }
        if (StrUtil.isBlank(json.getStr("allowedExtensions"))) {
            throw new IllegalArgumentException("允许扩展名不能为空");
        }
    }

    private void validateRateLimitConfig(JSONObject json) {
        validateRate(json.getInt("captchaPerIpMinute", 40), "验证码接口");
        validateRate(json.getInt("loginPerIpMinute", 30), "登录接口");
        validateRate(json.getInt("registerPerIpMinute", 10), "注册接口");
    }

    private void validateRate(int n, String label) {
        if (n < 0 || n > 200) {
            throw new IllegalArgumentException(label + "每分钟限流须在 0～200 之间（0 表示不限制）");
        }
    }

    private void validateLoginConfig(JSONObject json) {
        boolean captchaEnabled = json.getBool("captchaEnabled", true);
        if (captchaEnabled) {
            String type = json.getStr("captchaType", SystemConfigHelper.CAPTCHA_TYPE_IMAGE);
            if (StrUtil.isBlank(type) || !LOGIN_CAPTCHA_TYPES.contains(type)) {
                throw new IllegalArgumentException("验证码类型仅支持 image 或 slider");
            }
        }
        int maxRetry = json.getInt("maxRetryCount", 5);
        if (maxRetry < 1 || maxRetry > 20) {
            throw new IllegalArgumentException("最大重试次数须在 1～20 之间");
        }
        int lockTime = json.getInt("lockTime", 10);
        if (lockTime < 1 || lockTime > 120) {
            throw new IllegalArgumentException("锁定时长须在 1～120 分钟之间");
        }
    }

    private void validateRegisterConfig(JSONObject json) {
        if (json.containsKey("defaultRoleCode")) {
            String code = json.getStr("defaultRoleCode");
            if (StrUtil.isBlank(code)) {
                throw new IllegalArgumentException("默认角色编码不能为空");
            }
        }
        int minLen = json.getInt("minPasswordLength", 6);
        if (minLen < 6 || minLen > 32) {
            throw new IllegalArgumentException("密码最小长度须在 6～32 之间");
        }
    }
}

package com.admin.server.modules.system.api.config;

import com.admin.server.framework.security.api.RequirePermission;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.common.core.CommonResult;
import com.admin.server.common.core.PageResult;
import com.admin.server.framework.log.annotation.Log;
import com.admin.server.modules.system.dal.dataobject.config.SysConfigGroupDO;
import com.admin.server.modules.trade.dal.dataobject.sms.SmsLogDO;
import com.admin.server.modules.trade.framework.pay.PayServiceFactory;
import com.admin.server.modules.system.service.config.SysConfigGroupService;
import com.admin.server.modules.trade.service.sms.SmsLogService;
import com.admin.server.modules.trade.framework.sms.SmsServiceFactory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.admin.server.modules.ai.service.search.impl.CustomWebSearchExecutor;
import com.admin.server.modules.ai.service.search.impl.TavilyWebSearchService;
import com.admin.server.modules.ai.service.search.vo.CustomWebSearchEngineConfig;
import com.admin.server.modules.ai.service.search.vo.TavilyUsageVO;
import com.admin.server.modules.ai.service.search.vo.WebSearchTestResultVO;
import com.admin.server.modules.system.framework.security.SystemPermissionService;
import com.admin.server.modules.system.service.config.SystemConfigHelper;

@Tag(name = "系统配置")
@Slf4j
@RestController
@RequestMapping("/system/config-group")
public class SysConfigGroupController {

    private static final String MASK_PLACEHOLDER = "••••••••••••••••••••••••••••";

    /**
     * 各配置分组中真正需要脱敏的密钥字段（支持点号嵌套路径）。
     * <p>列表/详情统一脱敏；保存时占位符、空值或缺失字段一律沿用旧值，
     * 从而避免“有修改权限就能回显明文”，也避免前端把占位符写回数据库。</p>
     */
    private static final Map<String, List<String>> SENSITIVE_FIELDS = Map.of(
            SystemConfigHelper.GROUP_FILE, List.of(
                    "minioSecretKey", "minioAccessKey", "cosSecretKey", "cosSecretId",
                    "ossAccessKeySecret", "ossAccessKeyId",
                    "accessKey", "secretKey", "accessKeyId", "accessKeySecret"),
            SystemConfigHelper.GROUP_SMS, List.of(
                    "accessKeySecret"),
            SystemConfigHelper.GROUP_PAYMENT, List.of(
                    "wechatPay.apiV3Key", "wechatPay.privateKey",
                    "alipay.privateKey"),
            SystemConfigHelper.GROUP_EMAIL, List.of(
                    "password"),
            SystemConfigHelper.GROUP_THIRD_PARTY, List.of(
                    "wechat.appSecret", "alipay.privateKey",
                    "github.clientSecret", "google.clientSecret",
                    "tavily.apiKey"),
            SystemConfigHelper.GROUP_SECURITY, List.of(
                    "sm4SecretKey", "sm3SignKey", "sm2PrivateKey")
    );

    @Resource
    private SysConfigGroupService configGroupService;

    @Resource
    private SystemPermissionService ss;

    @Resource
    private PayServiceFactory payServiceFactory;

    @Resource
    private SmsServiceFactory smsServiceFactory;

    @Resource
    private SmsLogService smsLogService;

    @Resource
    private com.admin.server.modules.trade.service.email.EmailCodeService emailCodeService;

    @Resource
    private com.admin.server.framework.storage.RoutingFileStorageService routingFileStorageService;

    @Resource
    private SystemConfigHelper systemConfigHelper;

    @Resource
    private TavilyWebSearchService tavilyWebSearchService;

    @Resource
    private CustomWebSearchExecutor customWebSearchExecutor;

    @GetMapping("/list")
    @Operation(summary = "配置分组列表")
    @RequirePermission(value = "system:config:list", type = RequirePermission.Type.READ)
    public CommonResult<List<SysConfigGroupDO>> list() {
        List<SysConfigGroupDO> list = configGroupService.listAll();
        // 配置列表始终脱敏：是否具备修改权限都不能回显密钥明文
        List<SysConfigGroupDO> maskedList = list.stream().map(this::maskConfigGroup).toList();
        return CommonResult.success(maskedList);
    }

    @GetMapping("/{groupCode}")
    @Operation(summary = "获取配置分组")
    @RequirePermission(value = "system:config:list", type = RequirePermission.Type.READ)
    public CommonResult<SysConfigGroupDO> get(@PathVariable String groupCode) {
        SysConfigGroupDO row = configGroupService.getByGroupCode(groupCode);
        if (row == null) {
            throw new BusinessException(404, "配置分组不存在");
        }
        return CommonResult.success(maskConfigGroup(row));
    }

    private SysConfigGroupDO maskConfigGroup(SysConfigGroupDO row) {
        if (row == null || StrUtil.isBlank(row.getConfigValue()) || !JSONUtil.isTypeJSON(row.getConfigValue())) {
            return row;
        }
        // 复制新实例，避免污染底层缓存
        SysConfigGroupDO masked = new SysConfigGroupDO();
        masked.setId(row.getId());
        masked.setGroupCode(row.getGroupCode());
        masked.setGroupName(row.getGroupName());
        masked.setRemark(row.getRemark());
        masked.setCreateTime(row.getCreateTime());
        masked.setUpdateTime(row.getUpdateTime());

        JSONObject json = JSONUtil.parseObj(row.getConfigValue());
        List<String> fields = SENSITIVE_FIELDS.get(row.getGroupCode());
        if (fields != null) {
            for (String path : fields) {
                maskJsonPath(json, path);
            }
        }
        if (SystemConfigHelper.GROUP_THIRD_PARTY.equals(row.getGroupCode())) {
            maskCustomWebSearchProviders(json);
        }
        masked.setConfigValue(json.toString());
        return masked;
    }

    /**
     * 按点号路径脱敏嵌套字段，例如 {@code wechatPay.apiV3Key}。
     */
    private void maskJsonPath(JSONObject json, String path) {
        JSONObject current = json;
        String[] parts = path.split("\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            current = current.getJSONObject(parts[i]);
            if (current == null) {
                return;
            }
        }
        String field = parts[parts.length - 1];
        if (!current.containsKey(field)) {
            return;
        }
        String val = current.getStr(field);
        if (StrUtil.isNotBlank(val)) {
            current.set(field, MASK_PLACEHOLDER);
        }
    }

    /**
     * 保存时合并敏感字段：占位符、空值或缺失字段一律沿用旧值，只有显式新值才覆盖。
     */
    private void mergeSensitiveFields(String groupCode, JSONObject newJson, JSONObject oldJson) {
        List<String> fields = SENSITIVE_FIELDS.get(groupCode);
        if (fields == null || fields.isEmpty() || oldJson == null) {
            return;
        }
        for (String path : fields) {
            String incoming = readJsonPath(newJson, path);
            if (incoming == null || incoming.isBlank() || isMaskedSecret(incoming)) {
                String existing = readJsonPath(oldJson, path);
                if (existing != null) {
                    writeJsonPath(newJson, path, existing);
                }
            }
        }
        if (SystemConfigHelper.GROUP_THIRD_PARTY.equals(groupCode)) {
            mergeCustomWebSearchProviders(newJson, oldJson);
        }
    }

    /** 对第三方配置中的自定义联网搜索引擎 API Key 统一脱敏，防止读取接口泄露明文。 */
    private void maskCustomWebSearchProviders(JSONObject root) {
        JSONObject webSearch = root.getJSONObject("webSearch");
        if (webSearch == null) {
            return;
        }
        JSONArray providers = webSearch.getJSONArray("customProviders");
        if (providers == null || providers.isEmpty()) {
            return;
        }
        for (int i = 0; i < providers.size(); i++) {
            JSONObject item = providers.getJSONObject(i);
            if (item != null && StrUtil.isNotBlank(item.getStr("apiKey"))) {
                item.set("apiKey", MASK_PLACEHOLDER);
            }
        }
    }

    /** 保存自定义联网搜索引擎列表时，按引擎 id 保留未修改或脱敏占位的原 API Key。 */
    private void mergeCustomWebSearchProviders(JSONObject newJson, JSONObject oldJson) {
        JSONObject newWs = newJson.getJSONObject("webSearch");
        JSONObject oldWs = oldJson.getJSONObject("webSearch");
        if (newWs == null || oldWs == null) {
            return;
        }
        JSONArray newProviders = newWs.getJSONArray("customProviders");
        JSONArray oldProviders = oldWs.getJSONArray("customProviders");
        if (newProviders == null || oldProviders == null || oldProviders.isEmpty()) {
            return;
        }
        Map<String, String> oldKeysById = new java.util.HashMap<>();
        for (int i = 0; i < oldProviders.size(); i++) {
            JSONObject oldItem = oldProviders.getJSONObject(i);
            if (oldItem != null && StrUtil.isNotBlank(oldItem.getStr("id"))
                    && StrUtil.isNotBlank(oldItem.getStr("apiKey"))) {
                oldKeysById.put(oldItem.getStr("id"), oldItem.getStr("apiKey"));
            }
        }
        for (int i = 0; i < newProviders.size(); i++) {
            JSONObject newItem = newProviders.getJSONObject(i);
            if (newItem == null) {
                continue;
            }
            String incoming = newItem.getStr("apiKey");
            if (incoming == null || incoming.isBlank() || isMaskedSecret(incoming)) {
                String existing = oldKeysById.get(newItem.getStr("id"));
                if (existing != null) {
                    newItem.set("apiKey", existing);
                }
            }
        }
    }

    private String readJsonPath(JSONObject json, String path) {
        JSONObject current = json;
        String[] parts = path.split("\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            current = current.getJSONObject(parts[i]);
            if (current == null) {
                return null;
            }
        }
        return current.getStr(parts[parts.length - 1]);
    }

    private void writeJsonPath(JSONObject json, String path, String value) {
        JSONObject current = json;
        String[] parts = path.split("\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            JSONObject next = current.getJSONObject(parts[i]);
            if (next == null) {
                next = new JSONObject();
                current.set(parts[i], next);
            }
            current = next;
        }
        current.set(parts[parts.length - 1], value);
    }

    /** 纯占位符（后端统一掩码或前端星号掩码）才视为未修改，避免误伤真实密码中的 * 字符。 */
    private boolean isMaskedSecret(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.contains("•") || trimmed.matches("^\\*+$");
    }

    @PutMapping("/{groupCode}")
    @Operation(summary = "更新配置分组")
    @RequirePermission("system:config:update")
    @Log(title = "系统配置", businessType = Log.BusinessType.UPDATE)
    public CommonResult<Boolean> update(@PathVariable String groupCode, @RequestBody Map<String, String> body) {
        String configValue = body.get("configValue");
        if (configValue == null || configValue.isBlank()) {
            throw new BusinessException(400, "configValue 不能为空");
        }
        JSONObject oldGroupJson = null;
        JSONObject newGroupJson = null;
        // 列表/详情已脱敏，保存时统一把占位符、空值或缺失的敏感字段还原为旧值，
        // 避免前端原样提交占位符导致真实密钥被覆盖。
        if (JSONUtil.isTypeJSON(configValue)) {
            newGroupJson = JSONUtil.parseObj(configValue);
            oldGroupJson = systemConfigHelper.getGroupJson(groupCode);
            mergeSensitiveFields(groupCode, newGroupJson, oldGroupJson);
            configValue = newGroupJson.toString();
        }
        com.admin.server.framework.storage.StorageConfig oldStorageConfig = null;
        com.admin.server.framework.storage.StorageConfig newStorageConfig = null;
        if (SystemConfigHelper.GROUP_FILE.equals(groupCode)) {
            if (newGroupJson == null) {
                try {
                    newGroupJson = JSONUtil.parseObj(configValue);
                } catch (Exception e) {
                    throw new BusinessException(400, "配置内容必须是合法 JSON");
                }
                configValue = newGroupJson.toString();
            }
            newStorageConfig = systemConfigHelper.parseStorageConfig(newGroupJson);
            oldStorageConfig = systemConfigHelper.getStorageConfig();
            // 只有存储引擎类型、地址、桶或凭据真正变化时才做连通性测试；
            // 仅修改上传大小/扩展名等策略时不应被远程存储的临时不可用阻塞。
            if (storageConfigChanged(oldStorageConfig, newStorageConfig)) {
                String errDetail = routingFileStorageService.testConnectionDetail(newStorageConfig);
                if (errDetail != null) {
                    throw new BusinessException(400, "存储配置连通性测试未通过: " + errDetail);
                }
            }
        }

        configGroupService.updateConfig(groupCode, configValue);
        if (SystemConfigHelper.GROUP_FILE.equals(groupCode)) {
            try {
                routingFileStorageService.reload(newStorageConfig != null
                        ? newStorageConfig
                        : systemConfigHelper.getStorageConfig());
            } catch (Exception e) {
                restoreStorageConfig(oldGroupJson, oldStorageConfig);
                throw new BusinessException(500, "存储配置应用失败，已回滚: " + e.getMessage());
            }
        }
        return CommonResult.success(true);
    }

    private boolean storageConfigChanged(com.admin.server.framework.storage.StorageConfig oldConfig,
                                         com.admin.server.framework.storage.StorageConfig newConfig) {
        if (oldConfig == null || newConfig == null) {
            return true;
        }
        return !Objects.equals(oldConfig.getType(), newConfig.getType())
                || !Objects.equals(oldConfig.getLocalPath(), newConfig.getLocalPath())
                || !Objects.equals(oldConfig.getMinioEndpoint(), newConfig.getMinioEndpoint())
                || !Objects.equals(oldConfig.getMinioAccessKey(), newConfig.getMinioAccessKey())
                || !Objects.equals(oldConfig.getMinioSecretKey(), newConfig.getMinioSecretKey())
                || !Objects.equals(oldConfig.getMinioBucket(), newConfig.getMinioBucket())
                || !Objects.equals(oldConfig.getCosSecretId(), newConfig.getCosSecretId())
                || !Objects.equals(oldConfig.getCosSecretKey(), newConfig.getCosSecretKey())
                || !Objects.equals(oldConfig.getCosRegion(), newConfig.getCosRegion())
                || !Objects.equals(oldConfig.getCosBucket(), newConfig.getCosBucket())
                || !Objects.equals(oldConfig.getOssEndpoint(), newConfig.getOssEndpoint())
                || !Objects.equals(oldConfig.getOssAccessKeyId(), newConfig.getOssAccessKeyId())
                || !Objects.equals(oldConfig.getOssAccessKeySecret(), newConfig.getOssAccessKeySecret())
                || !Objects.equals(oldConfig.getOssBucket(), newConfig.getOssBucket());
    }

    private void restoreStorageConfig(JSONObject oldGroupJson,
                                      com.admin.server.framework.storage.StorageConfig oldStorageConfig) {
        try {
            if (oldGroupJson != null) {
                configGroupService.updateConfig(SystemConfigHelper.GROUP_FILE, oldGroupJson.toString());
            }
            if (oldStorageConfig != null) {
                routingFileStorageService.reload(oldStorageConfig);
            }
        } catch (Exception restoreError) {
            log.warn("存储配置回滚失败: {}", restoreError.getMessage(), restoreError);
        }
    }

    @GetMapping("/storage")
    @Operation(summary = "获取文件对象存储配置")
    @RequirePermission(value = "system:config:list", type = RequirePermission.Type.READ)
    public CommonResult<com.admin.server.modules.system.api.config.vo.StorageConfigVO> getStorageConfig() {
        JSONObject fileJson = systemConfigHelper.getGroupJson(SystemConfigHelper.GROUP_FILE);
        com.admin.server.framework.storage.StorageConfig activeConfig = routingFileStorageService.getCurrentConfig();
        String activeType = routingFileStorageService.getStorageType();

        String minioSec = fileJson.getStr("minioSecretKey", fileJson.getStr("secretKey", ""));
        String cosSec = fileJson.getStr("cosSecretKey", "");
        String ossSec = fileJson.getStr("ossAccessKeySecret", fileJson.getStr("accessKeySecret", ""));

        String activeBucket = switch (activeType) {
            case "minio" -> activeConfig != null ? activeConfig.getMinioBucket() : "wu-admin";
            case "cos" -> activeConfig != null ? activeConfig.getCosBucket() : "";
            case "oss" -> activeConfig != null ? activeConfig.getOssBucket() : "";
            default -> activeConfig != null && StrUtil.isNotBlank(activeConfig.getLocalPath()) ? activeConfig.getLocalPath() : "./data/uploads";
        };

        String effectiveLocalPath = activeConfig != null && StrUtil.isNotBlank(activeConfig.getLocalPath())
                ? activeConfig.getLocalPath()
                : systemConfigHelper.getStorageConfig().getLocalPath();
        String effectiveType = StrUtil.isNotBlank(fileJson.getStr("type")) ? fileJson.getStr("type") : activeType;

        // 读取配置页不做实时连通性探测：避免存储不可用时打开页面就等待超时。
        // 前端“测试连通性”按钮会主动调用 /test-connection，这里复用最近一次结果。
        Boolean lastHealth = routingFileStorageService.getLastHealth();
        boolean healthy = lastHealth == null || lastHealth;

        com.admin.server.modules.system.api.config.vo.StorageConfigVO vo = com.admin.server.modules.system.api.config.vo.StorageConfigVO.builder()
                .type(effectiveType)
                .localPath(effectiveLocalPath)
                .localDomain(fileJson.getStr("localDomain", ""))
                .minioDomain(fileJson.getStr("minioDomain", ""))
                .minioEndpoint(fileJson.getStr("minioEndpoint", "http://127.0.0.1:9000"))
                .minioAccessKey(fileJson.getStr("minioAccessKey", fileJson.getStr("accessKey", "")))
                .minioBucket(fileJson.getStr("minioBucket", "wu-admin"))
                .minioSecretKeyConfigured(StrUtil.isNotBlank(minioSec))
                .cosRegion(fileJson.getStr("cosRegion", "ap-guangzhou"))
                .cosBucket(fileJson.getStr("cosBucket", ""))
                .cosSecretId(fileJson.getStr("cosSecretId", ""))
                .cosDomain(fileJson.getStr("cosDomain", ""))
                .cosSecretKeyConfigured(StrUtil.isNotBlank(cosSec))
                .ossEndpoint(fileJson.getStr("ossEndpoint", "oss-cn-hangzhou.aliyuncs.com"))
                .ossBucket(fileJson.getStr("ossBucket", ""))
                .ossAccessKeyId(fileJson.getStr("ossAccessKeyId", fileJson.getStr("accessKeyId", "")))
                .ossDomain(fileJson.getStr("ossDomain", ""))
                .ossAccessKeySecretConfigured(StrUtil.isNotBlank(ossSec))
                .maxSizeMb(systemConfigHelper.getFileMaxSizeMb())
                .allowedExtensions(systemConfigHelper.getFileAllowedExtensions())
                .activeType(activeType)
                .activeBucket(activeBucket)
                .healthy(healthy)
                .build();
        if (!ss.hasPermission("system:config:update")) {
            vo.setMinioEndpoint(MASK_PLACEHOLDER);
            vo.setMinioAccessKey(MASK_PLACEHOLDER);
            vo.setCosSecretId(MASK_PLACEHOLDER);
            vo.setOssAccessKeyId(MASK_PLACEHOLDER);
        }
        return CommonResult.success(vo);
    }

    @PutMapping("/storage")
    @Operation(summary = "修改文件对象存储配置并热重载")
    @RequirePermission("system:config:update")
    @Log(title = "系统配置", businessType = Log.BusinessType.UPDATE)
    public CommonResult<Boolean> updateStorageConfig(@RequestBody com.admin.server.modules.system.api.config.vo.StorageConfigDTO dto) {
        if (dto == null || StrUtil.isBlank(dto.getType())) {
            throw new BusinessException(400, "存储引擎类型不能为空");
        }
        JSONObject rawOld = systemConfigHelper.getGroupJson(SystemConfigHelper.GROUP_FILE);

        String minioEndpoint = resolveSecret(dto.getMinioEndpoint(), rawOld.getStr("minioEndpoint", "http://127.0.0.1:9000"));
        String minioAk = resolveSecret(dto.getMinioAccessKey(), rawOld.getStr("minioAccessKey", rawOld.getStr("accessKey", "")));
        String minioSec = Boolean.TRUE.equals(dto.getClearMinioSecretKey())
                ? "" : resolveSecret(dto.getMinioSecretKey(), rawOld.getStr("minioSecretKey", rawOld.getStr("secretKey", "")));
        String cosSid = resolveSecret(dto.getCosSecretId(), rawOld.getStr("cosSecretId", ""));
        String cosSec = Boolean.TRUE.equals(dto.getClearCosSecretKey())
                ? "" : resolveSecret(dto.getCosSecretKey(), rawOld.getStr("cosSecretKey", ""));
        String ossAkId = resolveSecret(dto.getOssAccessKeyId(), rawOld.getStr("ossAccessKeyId", rawOld.getStr("accessKeyId", "")));
        String ossSec = Boolean.TRUE.equals(dto.getClearOssAccessKeySecret())
                ? "" : resolveSecret(dto.getOssAccessKeySecret(), rawOld.getStr("ossAccessKeySecret", rawOld.getStr("accessKeySecret", "")));

        JSONObject newJson = new JSONObject();
        newJson.set("type", dto.getType().trim().toLowerCase());
        String localPath = StrUtil.isNotBlank(dto.getLocalPath())
                ? dto.getLocalPath().trim()
                : StrUtil.isNotBlank(rawOld.getStr("localPath"))
                ? rawOld.getStr("localPath").trim()
                : systemConfigHelper.getStorageConfig().getLocalPath();
        newJson.set("localPath", localPath);
        newJson.set("localDomain", StrUtil.nullToEmpty(dto.getLocalDomain()).trim());
        newJson.set("minioDomain", StrUtil.nullToEmpty(dto.getMinioDomain()).trim());
        newJson.set("minioEndpoint", minioEndpoint);
        newJson.set("minioAccessKey", minioAk);
        newJson.set("minioSecretKey", minioSec);
        newJson.set("minioBucket", StrUtil.isNotBlank(dto.getMinioBucket()) ? dto.getMinioBucket().trim() : "wu-admin");
        newJson.set("cosRegion", StrUtil.nullToEmpty(dto.getCosRegion()).trim());
        newJson.set("cosBucket", StrUtil.nullToEmpty(dto.getCosBucket()).trim());
        newJson.set("cosSecretId", cosSid);
        newJson.set("cosSecretKey", cosSec);
        newJson.set("cosDomain", StrUtil.nullToEmpty(dto.getCosDomain()).trim());
        newJson.set("ossEndpoint", StrUtil.nullToEmpty(dto.getOssEndpoint()).trim());
        newJson.set("ossBucket", StrUtil.nullToEmpty(dto.getOssBucket()).trim());
        newJson.set("ossAccessKeyId", ossAkId);
        newJson.set("ossAccessKeySecret", ossSec);
        newJson.set("ossDomain", StrUtil.nullToEmpty(dto.getOssDomain()).trim());

        int maxMb = dto.getMaxSizeMb() != null ? dto.getMaxSizeMb() : rawOld.getInt("maxSizeMb", 50);
        String allowed = StrUtil.isNotBlank(dto.getAllowedExtensions()) ? dto.getAllowedExtensions().trim() : rawOld.getStr("allowedExtensions", "jpg,jpeg,png,gif,webp");
        newJson.set("maxSizeMb", Math.max(1, Math.min(maxMb, SystemConfigHelper.PLATFORM_MAX_FILE_MB)));
        newJson.set("allowedExtensions", allowed);

        com.admin.server.framework.storage.StorageConfig targetConfig = com.admin.server.framework.storage.StorageConfig.builder()
                .type(newJson.getStr("type"))
                .localPath(newJson.getStr("localPath"))
                .localDomain(newJson.getStr("localDomain"))
                .minioDomain(newJson.getStr("minioDomain"))
                .minioEndpoint(minioEndpoint)
                .minioAccessKey(minioAk)
                .minioSecretKey(minioSec)
                .minioBucket(newJson.getStr("minioBucket"))
                .cosRegion(newJson.getStr("cosRegion"))
                .cosBucket(newJson.getStr("cosBucket"))
                .cosSecretId(cosSid)
                .cosSecretKey(cosSec)
                .cosDomain(newJson.getStr("cosDomain"))
                .ossEndpoint(newJson.getStr("ossEndpoint"))
                .ossBucket(newJson.getStr("ossBucket"))
                .ossAccessKeyId(ossAkId)
                .ossAccessKeySecret(ossSec)
                .ossDomain(newJson.getStr("ossDomain"))
                .build();

        com.admin.server.framework.storage.StorageConfig oldConfig = systemConfigHelper.getStorageConfig();
        if (storageConfigChanged(oldConfig, targetConfig)) {
            String errDetail = routingFileStorageService.testConnectionDetail(targetConfig);
            if (errDetail != null) {
                throw new BusinessException(400, "存储引擎连通性测试未通过: " + errDetail);
            }
        }

        configGroupService.updateConfig(SystemConfigHelper.GROUP_FILE, newJson.toString());
        try {
            routingFileStorageService.reload(targetConfig);
        } catch (Exception e) {
            restoreStorageConfig(rawOld, oldConfig);
            throw new BusinessException(500, "存储配置应用失败，已回滚: " + e.getMessage());
        }
        return CommonResult.success(true);
    }

    @PostMapping("/test-storage")
    @Operation(summary = "测试存储引擎连通性")
    @RequirePermission("system:config:update")
    public CommonResult<Boolean> testStorageConfig(@RequestBody com.admin.server.modules.system.api.config.vo.StorageConfigDTO dto) {
        if (dto == null || StrUtil.isBlank(dto.getType())) {
            throw new BusinessException(400, "存储类型不能为空");
        }
        JSONObject rawOld = systemConfigHelper.getGroupJson(SystemConfigHelper.GROUP_FILE);
        String minioEndpoint = resolveSecret(dto.getMinioEndpoint(), rawOld.getStr("minioEndpoint", "http://127.0.0.1:9000"));
        String minioAk = resolveSecret(dto.getMinioAccessKey(), rawOld.getStr("minioAccessKey", rawOld.getStr("accessKey", "")));
        String minioSec = resolveSecret(dto.getMinioSecretKey(), rawOld.getStr("minioSecretKey", rawOld.getStr("secretKey", "")));
        String cosSid = resolveSecret(dto.getCosSecretId(), rawOld.getStr("cosSecretId", ""));
        String cosSec = resolveSecret(dto.getCosSecretKey(), rawOld.getStr("cosSecretKey", ""));
        String ossAkId = resolveSecret(dto.getOssAccessKeyId(), rawOld.getStr("ossAccessKeyId", rawOld.getStr("accessKeyId", "")));
        String ossSec = resolveSecret(dto.getOssAccessKeySecret(), rawOld.getStr("ossAccessKeySecret", rawOld.getStr("accessKeySecret", "")));

        com.admin.server.framework.storage.StorageConfig probeConfig = com.admin.server.framework.storage.StorageConfig.builder()
                .type(dto.getType().trim().toLowerCase())
                .localPath(StrUtil.isNotBlank(dto.getLocalPath())
                        ? dto.getLocalPath().trim()
                        : systemConfigHelper.getStorageConfig().getLocalPath())
                .localDomain(StrUtil.nullToEmpty(dto.getLocalDomain()).trim())
                .minioDomain(StrUtil.nullToEmpty(dto.getMinioDomain()).trim())
                .minioEndpoint(minioEndpoint)
                .minioAccessKey(minioAk)
                .minioSecretKey(minioSec)
                .minioBucket(StrUtil.isNotBlank(dto.getMinioBucket()) ? dto.getMinioBucket().trim() : "wu-admin")
                .cosRegion(StrUtil.nullToEmpty(dto.getCosRegion()).trim())
                .cosBucket(StrUtil.nullToEmpty(dto.getCosBucket()).trim())
                .cosSecretId(cosSid)
                .cosSecretKey(cosSec)
                .cosDomain(StrUtil.nullToEmpty(dto.getCosDomain()).trim())
                .ossEndpoint(StrUtil.nullToEmpty(dto.getOssEndpoint()).trim())
                .ossBucket(StrUtil.nullToEmpty(dto.getOssBucket()).trim())
                .ossAccessKeyId(ossAkId)
                .ossAccessKeySecret(ossSec)
                .ossDomain(StrUtil.nullToEmpty(dto.getOssDomain()).trim())
                .build();

        String errDetail = routingFileStorageService.testConnectionDetail(probeConfig);
        if (errDetail != null) {
            throw new BusinessException(400, "存储引擎连通性测试未通过: " + errDetail);
        }
        return CommonResult.success(true);
    }

    private String resolveSecret(String incoming, String existing) {
        if (incoming == null || incoming.isBlank() || incoming.contains("•") || incoming.contains("*")) {
            return existing != null ? existing : "";
        }
        return incoming.trim();
    }

    @Operation(summary = "创建测试支付订单")
    @PostMapping("/test-payment")
    @RequirePermission("system:config:update")
    public CommonResult<Map<String, String>> testPayment(@RequestBody TestPaymentRequest request) {
        if (request.getType() == null || request.getType().isBlank()) {
            throw new BusinessException(400, "支付类型不能为空");
        }
        String type = request.getType().trim();
        if (!payServiceFactory.isSupported(type)) {
            throw new BusinessException(400, "不支持的支付类型: " + type);
        }
        return CommonResult.success(payServiceFactory.createTestOrder(type));
    }

    @Data
    public static class TestPaymentRequest {
        /** wechat 或 alipay */
        private String type;
    }

    @Operation(summary = "测试发送短信")
    @PostMapping("/test-sms")
    @RequirePermission("system:config:update")
    public CommonResult<Boolean> testSms(@RequestBody TestSmsRequest request) {
        if (request.getPhone() == null || !request.getPhone().matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(400, "请输入正确的手机号");
        }
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        boolean success = smsServiceFactory.sendCode(request.getPhone(), code, request.getTemplateCode());
        if (success) {
            return CommonResult.success(true);
        }
        LambdaQueryWrapper<SmsLogDO> logQuery = new LambdaQueryWrapper<>();
        logQuery.eq(SmsLogDO::getPhone, request.getPhone())
                .orderByDesc(SmsLogDO::getCreateTime)
                .last("LIMIT 1");
        SmsLogDO latest = smsLogService.getOne(logQuery);
        String detail = latest != null && latest.getResultMsg() != null && !latest.getResultMsg().isBlank()
                ? latest.getResultMsg()
                : "请检查签名、模板 ID 与密钥是否正确";
        throw new BusinessException(500, "短信发送失败：" + detail);
    }

    @Operation(summary = "最近短信发送记录")
    @GetMapping("/sms-logs/recent")
    @RequirePermission(value = "system:config:list", type = RequirePermission.Type.READ)
    public CommonResult<List<SmsLogDO>> getRecentSmsLogs(
            @RequestParam(defaultValue = "5") Integer limit) {
        int size = limit == null || limit < 1 ? 5 : Math.min(limit, 50);
        LambdaQueryWrapper<SmsLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SmsLogDO::getCreateTime).last("LIMIT " + size);
        return CommonResult.success(smsLogService.list(wrapper));
    }

    @Operation(summary = "分页查询短信发送记录")
    @GetMapping("/sms-logs")
    @RequirePermission(value = "system:config:list", type = RequirePermission.Type.READ)
    public CommonResult<PageResult<SmsLogDO>> getSmsLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer status) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : Math.min(size, 100);
        LambdaQueryWrapper<SmsLogDO> wrapper = new LambdaQueryWrapper<>();
        if (phone != null && !phone.isBlank()) {
            wrapper.like(SmsLogDO::getPhone, phone.trim());
        }
        if (status != null) {
            wrapper.eq(SmsLogDO::getStatus, status);
        }
        wrapper.orderByDesc(SmsLogDO::getCreateTime);
        Page<SmsLogDO> result = smsLogService.page(new Page<>(pageNo, pageSize), wrapper);
        return CommonResult.success(PageResult.of(result.getRecords(), result.getTotal()));
    }

    @Data
    public static class TestSmsRequest {
        private String phone;
        /** 测试指定模板 CODE，为空则使用登录/注册模板 */
        private String templateCode;
    }

    @Operation(summary = "测试发送邮件")
    @PostMapping("/test-email")
    @RequirePermission("system:config:update")
    public CommonResult<Boolean> testEmail(@RequestBody TestEmailRequest request) {
        if (request.getToEmail() == null || !request.getToEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new BusinessException(400, "请输入正确的接收邮箱");
        }
        String err = emailCodeService.sendTestEmail(request.getToEmail().trim());
        if (err != null) {
            throw new BusinessException(500, "邮件发送失败：" + err);
        }
        return CommonResult.success(true);
    }

    @Data
    public static class TestEmailRequest {
        private String toEmail;
    }

    @Resource
    private com.admin.server.modules.trade.service.email.EmailLogService emailLogService;

    @Operation(summary = "最近邮件发送记录")
    @GetMapping("/email-logs/recent")
    @RequirePermission(value = "system:config:list", type = RequirePermission.Type.READ)
    public CommonResult<List<com.admin.server.modules.trade.dal.dataobject.email.EmailLogDO>> getRecentEmailLogs(
            @RequestParam(defaultValue = "5") Integer limit) {
        int size = limit == null || limit < 1 ? 5 : Math.min(limit, 50);
        LambdaQueryWrapper<com.admin.server.modules.trade.dal.dataobject.email.EmailLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(com.admin.server.modules.trade.dal.dataobject.email.EmailLogDO::getCreateTime).last("LIMIT " + size);
        return CommonResult.success(emailLogService.list(wrapper));
    }

    @Operation(summary = "分页查询邮件发送记录")
    @GetMapping("/email-logs")
    @RequirePermission(value = "system:config:list", type = RequirePermission.Type.READ)
    public CommonResult<PageResult<com.admin.server.modules.trade.dal.dataobject.email.EmailLogDO>> getEmailLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Integer status) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : Math.min(size, 100);
        LambdaQueryWrapper<com.admin.server.modules.trade.dal.dataobject.email.EmailLogDO> wrapper = new LambdaQueryWrapper<>();
        if (email != null && !email.isBlank()) {
            wrapper.like(com.admin.server.modules.trade.dal.dataobject.email.EmailLogDO::getEmail, email.trim());
        }
        if (status != null) {
            wrapper.eq(com.admin.server.modules.trade.dal.dataobject.email.EmailLogDO::getStatus, status);
        }
        wrapper.orderByDesc(com.admin.server.modules.trade.dal.dataobject.email.EmailLogDO::getCreateTime);
        Page<com.admin.server.modules.trade.dal.dataobject.email.EmailLogDO> result = emailLogService.page(new Page<>(pageNo, pageSize), wrapper);
        return CommonResult.success(PageResult.of(result.getRecords(), result.getTotal()));
    }

    @Operation(summary = "删除短信发送记录")
    @DeleteMapping("/sms-logs/{id}")
    @RequirePermission("system:config:update")
    public CommonResult<Boolean> deleteSmsLog(@PathVariable Long id) {
        smsLogService.removeById(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "批量删除短信发送记录")
    @DeleteMapping("/sms-logs/batch")
    @RequirePermission("system:config:update")
    public CommonResult<Boolean> deleteBatchSmsLogs(@RequestBody List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            smsLogService.removeByIds(ids);
        }
        return CommonResult.success(true);
    }

    @Operation(summary = "清空短信发送记录")
    @DeleteMapping("/sms-logs/clean")
    @RequirePermission("system:config:update")
    public CommonResult<Boolean> cleanSmsLogs() {
        smsLogService.remove(new LambdaQueryWrapper<>());
        return CommonResult.success(true);
    }

    @Operation(summary = "删除邮件发送记录")
    @DeleteMapping("/email-logs/{id}")
    @RequirePermission("system:config:update")
    public CommonResult<Boolean> deleteEmailLog(@PathVariable Long id) {
        emailLogService.removeById(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "批量删除邮件发送记录")
    @DeleteMapping("/email-logs/batch")
    @RequirePermission("system:config:update")
    public CommonResult<Boolean> deleteBatchEmailLogs(@RequestBody List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            emailLogService.removeByIds(ids);
        }
        return CommonResult.success(true);
    }

    @Operation(summary = "清空邮件发送记录")
    @DeleteMapping("/email-logs/clean")
    @RequirePermission("system:config:update")
    public CommonResult<Boolean> cleanEmailLogs() {
        emailLogService.remove(new LambdaQueryWrapper<>());
        return CommonResult.success(true);
    }

    @Operation(summary = "测试 Tavily 连通性并查询实时额度")
    @PostMapping("/web-search/tavily-usage")
    @RequirePermission("system:config:update")
    public CommonResult<TavilyUsageVO> queryTavilyUsage(@RequestBody(required = false) TavilyUsageRequest req) {
        String apiKey = req != null ? req.getApiKey() : null;
        if (StrUtil.isBlank(apiKey) || isMaskedSecret(apiKey)) {
            apiKey = null;
        }
        return CommonResult.success(tavilyWebSearchService.queryUsage(apiKey));
    }

    @Operation(summary = "试搜测试自定义联网搜索引擎")
    @PostMapping("/web-search/test-custom")
    @RequirePermission("system:config:update")
    public CommonResult<WebSearchTestResultVO> testCustomWebSearch(@RequestBody TestCustomWebSearchRequest req) {
        if (req == null || req.getEngine() == null) {
            throw new BusinessException(400, "搜索引擎配置不能为空");
        }
        return CommonResult.success(customWebSearchExecutor.testEngine(req.getEngine(), req.getQuery()));
    }

    @Data
    public static class TavilyUsageRequest {
        private String apiKey;
    }

    @Data
    public static class TestCustomWebSearchRequest {
        private CustomWebSearchEngineConfig engine;
        private String query;
    }
}

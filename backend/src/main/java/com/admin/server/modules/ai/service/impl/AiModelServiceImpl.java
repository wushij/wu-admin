package com.admin.server.modules.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.admin.server.common.core.PageResult;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.ai.api.vo.AiModelListModelsReqVO;
import com.admin.server.modules.ai.api.vo.AiModelPageReqVO;
import com.admin.server.modules.ai.api.vo.AiModelRespVO;
import com.admin.server.modules.ai.api.vo.AiModelSaveReqVO;
import com.admin.server.modules.ai.api.vo.AiModelTestReqVO;
import com.admin.server.modules.ai.convert.AiModelConvert;
import com.admin.server.modules.ai.dal.dataobject.AiModelDO;
import com.admin.server.modules.ai.dal.mysql.AiModelMapper;
import com.admin.server.modules.ai.providers.AiProviderFactory;
import com.admin.server.modules.ai.service.AiModelService;
import com.admin.server.modules.ai.util.AiKeyCrypto;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AiModelServiceImpl extends ServiceImpl<AiModelMapper, AiModelDO> implements AiModelService {

    /** 供应商标识只用于展示与筛选，允许任意取值，真正决定请求形态的是 protocol */

    /** 接入协议白名单：OpenAI 兼容 / Anthropic Messages */
    private static final Set<String> SUPPORTED_PROTOCOLS = Set.of("openai", "anthropic");

    /** 单次回复最大 Token 默认值与允许区间（与前端输入框保持一致） */
    private static final int DEFAULT_MAX_TOKENS = 8192;
    private static final int MIN_MAX_TOKENS = 128;
    private static final int MAX_MAX_TOKENS = 32768;

    @Resource
    private AiKeyCrypto aiKeyCrypto;

    @Resource
    private AiProviderFactory aiProviderFactory;

    @Override
    public PageResult<AiModelRespVO> page(AiModelPageReqVO reqVO) {
        Page<AiModelDO> page = new Page<>(reqVO.getPageNo(), reqVO.getPageSize());
        LambdaQueryWrapper<AiModelDO> wrapper = new LambdaQueryWrapper<AiModelDO>()
                .like(StrUtil.isNotBlank(reqVO.getName()), AiModelDO::getName, reqVO.getName())
                .eq(StrUtil.isNotBlank(reqVO.getProvider()), AiModelDO::getProvider, reqVO.getProvider())
                .eq(reqVO.getStatus() != null, AiModelDO::getStatus, reqVO.getStatus())
                .orderByDesc(AiModelDO::getIsDefault)
                .orderByDesc(AiModelDO::getUpdateTime);
        Page<AiModelDO> result = page(page, wrapper);
        List<AiModelRespVO> voList = new ArrayList<>(result.getRecords().size());
        for (AiModelDO model : result.getRecords()) {
            voList.add(AiModelConvert.convertModel(model, decryptApiKey(model)));
        }
        return PageResult.of(voList, result.getTotal());
    }

    @Override
    public List<AiModelRespVO> listEnabled() {
        List<AiModelDO> list = list(new LambdaQueryWrapper<AiModelDO>()
                .eq(AiModelDO::getStatus, 1)
                .orderByDesc(AiModelDO::getIsDefault)
                .orderByAsc(AiModelDO::getId));
        List<AiModelRespVO> voList = new ArrayList<>(list.size());
        for (AiModelDO model : list) {
            // 列表选择场景不需要密钥掩码，避免逐行解密开销
            voList.add(AiModelConvert.convertModel(model, null));
        }
        return voList;
    }

    @Override
    public AiModelRespVO detail(Long id) {
        AiModelDO model = getModelOrThrow(id);
        return AiModelConvert.convertModel(model, decryptApiKey(model));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(AiModelSaveReqVO reqVO) {
        validateSaveReq(reqVO);
        AiModelDO model = normalizeModel(AiModelConvert.convertSave(reqVO));
        model.setId(null);
        model.setApiKey(aiKeyCrypto.encrypt(StrUtil.trim(reqVO.getApiKey())));
        if (model.getProtocol() == null) {
            model.setProtocol("openai");
        }
        if (model.getStatus() == null) {
            model.setStatus(1);
        }
        if (model.getTemperature() == null) {
            model.setTemperature(0.7);
        }
        if (model.getMaxTokens() == null) {
            model.setMaxTokens(DEFAULT_MAX_TOKENS);
        }
        if (model.getIsDefault() == null) {
            model.setIsDefault(0);
        }
        // 首个模型自动设为默认
        if (count() == 0) {
            model.setIsDefault(1);
        }
        if (model.getIsDefault() == 1) {
            clearDefault();
        }
        save(model);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AiModelSaveReqVO reqVO) {
        if (reqVO.getId() == null) {
            throw new BusinessException("模型配置ID不能为空");
        }
        validateSaveReq(reqVO);
        AiModelDO old = getModelOrThrow(reqVO.getId());
        AiModelDO model = normalizeModel(AiModelConvert.convertSave(reqVO));
        // apiKey 留空表示保持原密钥不变
        if (StrUtil.isNotBlank(reqVO.getApiKey())) {
            model.setApiKey(aiKeyCrypto.encrypt(StrUtil.trim(reqVO.getApiKey())));
        } else {
            model.setApiKey(old.getApiKey());
        }
        // 默认模型不能被直接取消默认，否则会出现“没有任何默认模型”的中间状态
        if (old.getIsDefault() != null && old.getIsDefault() == 1
                && model.getIsDefault() != null && model.getIsDefault() == 0) {
            throw new BusinessException("默认模型不可取消默认，请先将其他模型设为默认");
        }
        if (model.getIsDefault() != null && model.getIsDefault() == 1 && old.getIsDefault() != 1) {
            clearDefault();
        }
        updateById(model);
    }

    @Override
    public void delete(Long id) {
        AiModelDO model = getModelOrThrow(id);
        if (model.getIsDefault() != null && model.getIsDefault() == 1) {
            throw new BusinessException("默认模型不可删除，请先将其他模型设为默认");
        }
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        AiModelDO model = getModelOrThrow(id);
        if (model.getStatus() == null || model.getStatus() != 1) {
            throw new BusinessException("禁用中的模型不能设为默认");
        }
        clearDefault();
        update(new LambdaUpdateWrapper<AiModelDO>()
                .eq(AiModelDO::getId, id)
                .set(AiModelDO::getIsDefault, 1));
    }

    @Override
    public long test(AiModelTestReqVO reqVO) {
        ProbeTarget target = buildProbeTarget(reqVO.getId(), reqVO.getProvider(), reqVO.getProtocol(),
                reqVO.getModelName(), reqVO.getBaseUrl(), reqVO.getPathTemplate(),
                reqVO.getExtraHeaders(), reqVO.getApiKey());
        AiModelDO model = target.model();
        String plainKey = target.plainApiKey();
        if (StrUtil.isBlank(model.getProvider()) || StrUtil.isBlank(model.getBaseUrl())
                || StrUtil.isBlank(model.getModelName())) {
            throw new BusinessException("供应商、模型名称与 BaseUrl 不能为空");
        }
        // 协议留空时按默认 openai 处理，与新增/编辑保持一致；
        // Set.of(...).contains(null) 会抛 NPE，这里必须先兜底再判断
        model.setProtocol(normalizeProtocol(model.getProtocol()));
        if (!SUPPORTED_PROTOCOLS.contains(model.getProtocol())) {
            throw new BusinessException("接入协议不合法，仅支持 openai/anthropic");
        }
        validateBaseUrl(model.getBaseUrl());
        validateExtraHeaders(model.getExtraHeaders());
        try {
            return aiProviderFactory.getStrategy(model).testConnection(model, plainKey);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("连通性测试失败: " + e.getMessage());
        }
    }

    @Override
    public List<String> listModels(AiModelListModelsReqVO reqVO) {
        ProbeTarget target = buildProbeTarget(reqVO.getId(), reqVO.getProvider(), reqVO.getProtocol(),
                null, reqVO.getBaseUrl(), reqVO.getPathTemplate(),
                reqVO.getExtraHeaders(), reqVO.getApiKey());
        AiModelDO model = target.model();
        if (StrUtil.isBlank(model.getProvider()) || StrUtil.isBlank(model.getBaseUrl())) {
            throw new BusinessException("供应商与接口地址不能为空");
        }
        // 协议留空时按默认 openai 处理，与新增/编辑保持一致
        model.setProtocol(normalizeProtocol(model.getProtocol()));
        if (!SUPPORTED_PROTOCOLS.contains(model.getProtocol())) {
            throw new BusinessException("接入协议不合法，仅支持 openai/anthropic");
        }
        validateBaseUrl(model.getBaseUrl());
        validateExtraHeaders(model.getExtraHeaders());
        try {
            return aiProviderFactory.getStrategy(model).listModels(model, target.plainApiKey());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("获取模型列表失败: " + e.getMessage());
        }
    }

    @Override
    public AiModelDO getAvailableModel(Long modelId) {
        AiModelDO model;
        if (modelId != null) {
            model = getById(modelId);
        } else {
            model = getOne(new LambdaQueryWrapper<AiModelDO>()
                    .eq(AiModelDO::getIsDefault, 1)
                    .last("LIMIT 1"));
        }
        if (model == null) {
            throw new BusinessException("尚未配置可用的 AI 模型，请联系管理员在「AI 模型配置」中添加");
        }
        if (model.getStatus() == null || model.getStatus() != 1) {
            throw new BusinessException("当前 AI 模型已被禁用，请联系管理员");
        }
        return model;
    }

    @Override
    public String decryptApiKey(AiModelDO model) {
        return model == null ? "" : aiKeyCrypto.decrypt(model.getApiKey());
    }

    private void validateSaveReq(AiModelSaveReqVO reqVO) {
        if (StrUtil.isBlank(reqVO.getName())) {
            throw new BusinessException("配置名称不能为空");
        }
        // 供应商标识只用于展示与筛选，不做白名单限制；真正决定请求形态的是 protocol
        String provider = normalizeProvider(reqVO.getProvider());
        if (StrUtil.isBlank(provider)) {
            throw new BusinessException("供应商不能为空");
        }
        String protocol = normalizeProtocol(reqVO.getProtocol());
        if (!SUPPORTED_PROTOCOLS.contains(protocol)) {
            throw new BusinessException("接入协议不合法，仅支持 openai/anthropic");
        }
        if (StrUtil.isBlank(reqVO.getModelName())) {
            throw new BusinessException("模型名称不能为空");
        }
        if (StrUtil.isBlank(reqVO.getBaseUrl())) {
            throw new BusinessException("BaseUrl 不能为空");
        }
        // 新增时必须提供 API Key；编辑时留空表示沿用库中密钥
        if (reqVO.getId() == null && StrUtil.isBlank(reqVO.getApiKey())) {
            throw new BusinessException("API Key 不能为空");
        }
        if (reqVO.getTemperature() != null && (reqVO.getTemperature() < 0 || reqVO.getTemperature() > 2)) {
            throw new BusinessException("采样温度取值范围为 0 ~ 2");
        }
        if (reqVO.getMaxTokens() != null
                && (reqVO.getMaxTokens() < MIN_MAX_TOKENS || reqVO.getMaxTokens() > MAX_MAX_TOKENS)) {
            throw new BusinessException("最大 Tokens 取值范围为 " + MIN_MAX_TOKENS + " ~ " + MAX_MAX_TOKENS);
        }
        validateBaseUrl(reqVO.getBaseUrl());
        validateExtraHeaders(reqVO.getExtraHeaders());
    }

    /** 供应商、协议、地址等统一去除首尾空格；协议与供应商标识统一小写，避免筛选用大小写不一致 */
    private AiModelDO normalizeModel(AiModelDO model) {
        model.setName(StrUtil.trim(model.getName()));
        // 局部更新（如只切启用状态）可能不带 provider/protocol，此时保持原值
        if (StrUtil.isNotBlank(model.getProvider())) {
            model.setProvider(normalizeProvider(model.getProvider()));
        }
        if (StrUtil.isNotBlank(model.getProtocol())) {
            model.setProtocol(normalizeProtocol(model.getProtocol()));
        }
        model.setModelName(StrUtil.trim(model.getModelName()));
        model.setBaseUrl(StrUtil.trim(model.getBaseUrl()));
        if (model.getPathTemplate() != null) {
            model.setPathTemplate(StrUtil.trim(model.getPathTemplate()));
        }
        if (model.getExtraHeaders() != null) {
            model.setExtraHeaders(StrUtil.trim(model.getExtraHeaders()));
        }
        return model;
    }

    private String normalizeProvider(String provider) {
        return StrUtil.nullToEmpty(StrUtil.trim(provider)).toLowerCase();
    }

    private String normalizeProtocol(String protocol) {
        String normalized = StrUtil.nullToEmpty(StrUtil.trim(protocol)).toLowerCase();
        return StrUtil.blankToDefault(normalized, "openai");
    }

    /**
     * 校验接口地址。
     * <p>只限制协议、userinfo 与云元数据地址，不封禁内网和 localhost：
     * 本项目明确支持接入本地 Ollama、局域网 vLLM 等自建模型。</p>
     */
    private void validateBaseUrl(String baseUrl) {
        URI uri;
        try {
            uri = URI.create(baseUrl.trim());
        } catch (Exception e) {
            throw new BusinessException("接口地址不是合法的 URL");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw new BusinessException("接口地址必须以 http:// 或 https:// 开头");
        }
        if (uri.getUserInfo() != null) {
            throw new BusinessException("接口地址不允许携带用户名密码");
        }
        String host = StrUtil.trim(uri.getHost());
        if (StrUtil.isBlank(host)) {
            throw new BusinessException("接口地址缺少主机名");
        }
        String normalized = host.toLowerCase();
        if ("169.254.169.254".equals(normalized) || "metadata.google.internal".equals(normalized)
                || "0.0.0.0".equals(normalized)) {
            throw new BusinessException("接口地址不允许指向云元数据或通配地址");
        }
    }

    private void validateExtraHeaders(String extraHeaders) {
        if (StrUtil.isBlank(extraHeaders)) {
            return;
        }
        try {
            JSONObject headers = JSONUtil.parseObj(extraHeaders);
            for (String name : headers.keySet()) {
                if ("authorization".equalsIgnoreCase(name) || "x-api-key".equalsIgnoreCase(name)
                        || "api-key".equalsIgnoreCase(name) || "content-type".equalsIgnoreCase(name)) {
                    throw new BusinessException("额外请求头不允许覆盖鉴权或 Content-Type 头: " + name);
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("额外请求头必须是合法的 JSON 对象");
        }
    }

    private AiModelDO getModelOrThrow(Long id) {
        AiModelDO model = getById(id);
        if (model == null) {
            throw new BusinessException(404, "模型配置不存在");
        }
        return model;
    }

    /**
     * 组装探测用的临时模型配置：传 id 时以库中配置为底、表单值覆盖；否则完全用表单值。
     * <p>apiKey 留空且传了 id 时复用库中已保存的密钥，避免编辑页被迫重新输入。</p>
     */
    private ProbeTarget buildProbeTarget(Long id, String provider, String protocol, String modelName,
                                         String baseUrl, String pathTemplate, String extraHeaders, String apiKey) {
        AiModelDO model;
        String plainKey;
        if (id != null) {
            model = getModelOrThrow(id);
            // 表单修改了字段时以表单值优先
            if (StrUtil.isNotBlank(provider)) {
                model.setProvider(provider);
            }
            if (StrUtil.isNotBlank(protocol)) {
                model.setProtocol(protocol);
            }
            if (StrUtil.isNotBlank(modelName)) {
                model.setModelName(modelName);
            }
            if (StrUtil.isNotBlank(baseUrl)) {
                model.setBaseUrl(baseUrl);
            }
            if (pathTemplate != null) {
                model.setPathTemplate(pathTemplate);
            }
            if (extraHeaders != null) {
                model.setExtraHeaders(extraHeaders);
            }
            plainKey = StrUtil.isNotBlank(apiKey) ? apiKey : decryptApiKey(model);
        } else {
            model = new AiModelDO();
            model.setProvider(provider);
            model.setProtocol(protocol);
            model.setModelName(modelName);
            model.setBaseUrl(baseUrl);
            model.setPathTemplate(pathTemplate);
            model.setExtraHeaders(extraHeaders);
            plainKey = apiKey;
        }
        return new ProbeTarget(normalizeModel(model), plainKey);
    }

    /** 探测类接口（连通性测试 / 获取模型列表）的临时配置与明文密钥 */
    private record ProbeTarget(AiModelDO model, String plainApiKey) {
    }

    private void clearDefault() {
        update(new LambdaUpdateWrapper<AiModelDO>()
                .eq(AiModelDO::getIsDefault, 1)
                .set(AiModelDO::getIsDefault, 0));
    }
}

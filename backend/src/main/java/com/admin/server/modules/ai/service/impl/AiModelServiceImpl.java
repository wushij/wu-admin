package com.admin.server.modules.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.admin.server.common.core.PageResult;
import com.admin.server.common.exception.BusinessException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AiModelServiceImpl extends ServiceImpl<AiModelMapper, AiModelDO> implements AiModelService {

    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("deepseek", "openai", "qwen", "kimi");

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
        AiModelDO model = AiModelConvert.convertSave(reqVO);
        model.setId(null);
        model.setApiKey(aiKeyCrypto.encrypt(StrUtil.trim(reqVO.getApiKey())));
        if (model.getStatus() == null) {
            model.setStatus(1);
        }
        if (model.getTemperature() == null) {
            model.setTemperature(0.7);
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
        AiModelDO model = AiModelConvert.convertSave(reqVO);
        // apiKey 留空表示保持原密钥不变
        if (StrUtil.isNotBlank(reqVO.getApiKey())) {
            model.setApiKey(aiKeyCrypto.encrypt(StrUtil.trim(reqVO.getApiKey())));
        } else {
            model.setApiKey(old.getApiKey());
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
        AiModelDO model;
        String plainKey;
        if (reqVO.getId() != null) {
            model = getModelOrThrow(reqVO.getId());
            // 表单修改了字段时以表单值优先测试
            if (StrUtil.isNotBlank(reqVO.getProvider())) {
                model.setProvider(reqVO.getProvider());
            }
            if (StrUtil.isNotBlank(reqVO.getModelName())) {
                model.setModelName(reqVO.getModelName());
            }
            if (StrUtil.isNotBlank(reqVO.getBaseUrl())) {
                model.setBaseUrl(reqVO.getBaseUrl());
            }
            plainKey = StrUtil.isNotBlank(reqVO.getApiKey()) ? reqVO.getApiKey() : decryptApiKey(model);
        } else {
            model = new AiModelDO();
            model.setProvider(reqVO.getProvider());
            model.setModelName(reqVO.getModelName());
            model.setBaseUrl(reqVO.getBaseUrl());
            plainKey = reqVO.getApiKey();
        }
        if (StrUtil.isBlank(model.getProvider()) || StrUtil.isBlank(model.getBaseUrl())) {
            throw new BusinessException("供应商与 BaseUrl 不能为空");
        }
        try {
            return aiProviderFactory.getStrategy(model.getProvider()).testConnection(model, plainKey);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("连通性测试失败: " + e.getMessage());
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
        if (StrUtil.isBlank(reqVO.getProvider()) || !SUPPORTED_PROVIDERS.contains(reqVO.getProvider().toLowerCase())) {
            throw new BusinessException("供应商不合法，仅支持 deepseek/openai/qwen/kimi");
        }
        if (StrUtil.isBlank(reqVO.getModelName())) {
            throw new BusinessException("模型名称不能为空");
        }
        if (StrUtil.isBlank(reqVO.getBaseUrl())) {
            throw new BusinessException("BaseUrl 不能为空");
        }
        if (reqVO.getTemperature() != null && (reqVO.getTemperature() < 0 || reqVO.getTemperature() > 2)) {
            throw new BusinessException("采样温度取值范围为 0 ~ 2");
        }
    }

    private AiModelDO getModelOrThrow(Long id) {
        AiModelDO model = getById(id);
        if (model == null) {
            throw new BusinessException(404, "模型配置不存在");
        }
        return model;
    }

    private void clearDefault() {
        update(new LambdaUpdateWrapper<AiModelDO>()
                .eq(AiModelDO::getIsDefault, 1)
                .set(AiModelDO::getIsDefault, 0));
    }
}

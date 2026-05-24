package com.admin.server.modules.ai.service;

import com.admin.server.common.core.PageResult;
import com.admin.server.modules.ai.api.vo.AiModelPageReqVO;
import com.admin.server.modules.ai.api.vo.AiModelRespVO;
import com.admin.server.modules.ai.api.vo.AiModelSaveReqVO;
import com.admin.server.modules.ai.api.vo.AiModelTestReqVO;
import com.admin.server.modules.ai.dal.dataobject.AiModelDO;

import java.util.List;

/**
 * AI 模型供应商配置管理
 */
public interface AiModelService {

    PageResult<AiModelRespVO> page(AiModelPageReqVO reqVO);

    /** 启用中的模型列表（供对话面板模型选择器使用） */
    List<AiModelRespVO> listEnabled();

    AiModelRespVO detail(Long id);

    void create(AiModelSaveReqVO reqVO);

    void update(AiModelSaveReqVO reqVO);

    void delete(Long id);

    /** 设为全局默认模型（唯一性由本方法保证） */
    void setDefault(Long id);

    /** 连通性测试，返回往返延迟毫秒数 */
    long test(AiModelTestReqVO reqVO);

    /** 获取可用模型：modelId 为空时取默认模型，均校验启用状态 */
    AiModelDO getAvailableModel(Long modelId);

    /** 解密模型的 API Key 明文 */
    String decryptApiKey(AiModelDO model);
}

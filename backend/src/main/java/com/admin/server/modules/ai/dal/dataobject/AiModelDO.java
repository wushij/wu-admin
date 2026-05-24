package com.admin.server.modules.ai.dal.dataobject;

import com.admin.server.common.mybatis.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 模型供应商配置 DO，映射 sys_ai_model
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ai_model")
public class AiModelDO extends BaseEntity {
    /** 配置名称（展示用，如「DeepSeek 官方」） */
    private String name;
    /** 供应商: deepseek/openai/qwen/kimi */
    private String provider;
    /** 模型名称（如 deepseek-chat / qwen-plus） */
    private String modelName;
    /** API 基础地址（不含 /chat/completions） */
    private String baseUrl;
    /** API 密钥（SM4-CBC 加密存储） */
    private String apiKey;
    /** 采样温度 0~2 */
    private Double temperature;
    /** 单次回复最大 Token 数 */
    private Integer maxTokens;
    /** 系统提示词（角色设定） */
    private String systemPrompt;
    /** 是否默认模型 0:否 1:是（全局唯一） */
    private Integer isDefault;
    /** 状态 0:禁用 1:启用 */
    private Integer status;
    /** 备注 */
    private String remark;
}

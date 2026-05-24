package com.admin.server.modules.ai.api.vo;

import com.admin.server.common.core.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 模型配置分页查询入参
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiModelPageReqVO extends PageParam {
    private String name;
    private String provider;
    private Integer status;
}

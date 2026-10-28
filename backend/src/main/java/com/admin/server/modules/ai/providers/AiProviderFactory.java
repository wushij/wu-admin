package com.admin.server.modules.ai.providers;

import cn.hutool.core.util.StrUtil;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.ai.dal.dataobject.AiModelDO;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 大模型接入协议策略工厂
 */
@Component
public class AiProviderFactory {

    @Resource
    private List<AiProviderStrategy> strategies;

    /** 按模型配置里的协议取策略实现，为空时兼容为 openai */
    public AiProviderStrategy getStrategy(AiModelDO model) {
        return getStrategy(model == null ? null : model.getProtocol());
    }

    /** 按协议标识取策略实现，不支持时抛业务异常 */
    public AiProviderStrategy getStrategy(String protocol) {
        String key = StrUtil.blankToDefault(StrUtil.trim(protocol), "openai").toLowerCase();
        for (AiProviderStrategy strategy : strategies) {
            if (strategy.protocol().equalsIgnoreCase(key)) {
                return strategy;
            }
        }
        throw new BusinessException("不支持的接入协议: " + key + "，当前支持 openai / anthropic");
    }
}

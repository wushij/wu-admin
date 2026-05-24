package com.admin.server.modules.ai.providers;

import com.admin.server.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 大模型供应商策略工厂
 */
@Component
public class AiProviderFactory {

    @Resource
    private List<AiProviderStrategy> strategies;

    /** 根据供应商标识获取策略实现，不支持时抛业务异常 */
    public AiProviderStrategy getStrategy(String provider) {
        for (AiProviderStrategy strategy : strategies) {
            if (strategy.supports(provider)) {
                return strategy;
            }
        }
        throw new BusinessException("不支持的模型供应商: " + provider);
    }
}

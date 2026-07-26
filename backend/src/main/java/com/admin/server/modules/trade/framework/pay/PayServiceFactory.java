package com.admin.server.modules.trade.framework.pay;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayServiceFactory {

    private final List<PayService> payServices;
    private final PayOrderStore payOrderStore;

    private final Map<String, PayService> serviceMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (PayService service : payServices) {
            serviceMap.put(service.getPayType(), service);
            log.info("注册支付服务: {} - {}", service.getPayType(), service.getPayTypeName());
        }
    }

    public PayService getService(String payType) {
        PayService service = serviceMap.get(payType);
        if (service == null) {
            throw new RuntimeException("不支持的支付类型: " + payType);
        }
        return service;
    }

    public boolean isSupported(String payType) {
        return serviceMap.containsKey(payType);
    }

    public Map<String, String> createTestOrder(String payType) {
        Map<String, String> result = getService(payType).createTestOrder();
        payOrderStore.registerPending(result.get("orderNo"), payType);
        return result;
    }
}

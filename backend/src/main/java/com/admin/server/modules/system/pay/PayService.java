package com.admin.server.modules.system.pay;

import java.util.Map;

/**
 * 支付服务接口
 */
public interface PayService {

    Map<String, String> createTestOrder();

    String getPayType();

    String getPayTypeName();
}

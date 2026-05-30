package cn.rbac.server.modules.system.pay;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支付订单内存存储（测试支付与回调联调，生产环境应落库）
 */
@Component
public class PayOrderStore {

    private final ConcurrentHashMap<String, PayOrderRecord> orders = new ConcurrentHashMap<>();

    public void registerPending(String orderNo, String payType) {
        PayOrderRecord record = new PayOrderRecord();
        record.setOrderNo(orderNo);
        record.setPayType(payType);
        record.setStatus("PENDING");
        record.setCreateTime(LocalDateTime.now());
        orders.put(orderNo, record);
    }

    public void markPaid(String orderNo, String payType, String transactionId, String amount) {
        orders.compute(orderNo, (key, existing) -> {
            PayOrderRecord record = existing != null ? existing : new PayOrderRecord();
            record.setOrderNo(orderNo);
            record.setPayType(payType);
            record.setStatus("PAID");
            record.setTransactionId(transactionId);
            record.setAmount(amount);
            if (record.getCreateTime() == null) {
                record.setCreateTime(LocalDateTime.now());
            }
            record.setPaidTime(LocalDateTime.now());
            return record;
        });
    }

    public PayOrderRecord get(String orderNo) {
        return orders.get(orderNo);
    }
}

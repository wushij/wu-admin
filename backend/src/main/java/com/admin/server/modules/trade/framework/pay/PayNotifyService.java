package com.admin.server.modules.trade.framework.pay;

import cn.hutool.json.JSONObject;
import com.alipay.api.internal.util.AlipaySignature;
import com.wechat.pay.java.core.notification.AutoCertificateNotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayNotifyService {

    private final PayOrderStore payOrderStore;
    private final WechatPayService wechatPayService;
    private final AlipayService alipayService;

    @SuppressWarnings("deprecation")
    public Map<String, String> handleWechatNotify(String serial, String nonce, String timestamp,
                                                   String signature, String body) {
        Map<String, String> fail = new HashMap<>();
        fail.put("code", "FAIL");
        try {
            JSONObject config = wechatPayService.getWechatPayConfig();
            if (config == null) {
                fail.put("message", "微信支付配置不存在");
                return fail;
            }

            AutoCertificateNotificationConfig notificationConfig = buildWechatNotificationConfig(config);
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(serial)
                    .nonce(nonce)
                    .signature(signature)
                    .timestamp(timestamp)
                    .body(body)
                    .build();

            NotificationParser parser = new NotificationParser(notificationConfig);
            Transaction transaction = parser.parse(requestParam, Transaction.class);

            if (Transaction.TradeStateEnum.SUCCESS.equals(transaction.getTradeState())) {
                String orderNo = transaction.getOutTradeNo();
                String transactionId = transaction.getTransactionId();
                String amount = transaction.getAmount() != null
                        ? String.valueOf(transaction.getAmount().getTotal()) : null;
                payOrderStore.markPaid(orderNo, "wechat", transactionId, amount);
                log.info("微信支付回调成功: orderNo={}, transactionId={}", orderNo, transactionId);
            }

            Map<String, String> ok = new HashMap<>();
            ok.put("code", "SUCCESS");
            ok.put("message", "成功");
            return ok;
        } catch (Exception e) {
            log.error("微信支付回调处理失败", e);
            fail.put("message", e.getMessage());
            return fail;
        }
    }

    public String handleAlipayNotify(Map<String, String> params) {
        try {
            JSONObject config = alipayService.getAlipayConfig();
            if (config == null) {
                log.error("支付宝配置不存在");
                return "failure";
            }

            String publicKey = config.getStr("publicKey", "");
            String signType = config.getStr("signType", "RSA2");
            if (!StringUtils.hasText(publicKey)) {
                log.error("支付宝公钥未配置");
                return "failure";
            }

            if (!AlipaySignature.rsaCheckV1(params, publicKey, "UTF-8", signType)) {
                log.error("支付宝回调验签失败: orderNo={}", params.get("out_trade_no"));
                return "failure";
            }

            String tradeStatus = params.get("trade_status");
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                payOrderStore.markPaid(params.get("out_trade_no"), "alipay",
                        params.get("trade_no"), params.get("total_amount"));
                log.info("支付宝回调成功: orderNo={}", params.get("out_trade_no"));
            }
            return "success";
        } catch (Exception e) {
            log.error("支付宝回调处理失败", e);
            return "failure";
        }
    }

    public PayOrderRecord getOrder(String orderNo) {
        return payOrderStore.get(orderNo);
    }

    /**
     * 查单并同步状态：已支付直接返回；待支付则主动请求微信/支付宝查单（本地开发回调不可达时依赖此逻辑）
     */
    public PayOrderRecord syncOrderStatus(String orderNo) {
        PayOrderRecord record = payOrderStore.get(orderNo);
        if (record == null) {
            return null;
        }
        if ("PAID".equals(record.getStatus())) {
            return record;
        }
        String payType = record.getPayType();
        if ("wechat".equals(payType) || orderNo.startsWith("WX")) {
            wechatPayService.syncPaidStatus(orderNo);
        } else if ("alipay".equals(payType) || orderNo.startsWith("ALI")) {
            alipayService.syncPaidStatus(orderNo);
        }
        return payOrderStore.get(orderNo);
    }

    @SuppressWarnings("deprecation")
    private AutoCertificateNotificationConfig buildWechatNotificationConfig(JSONObject config) {
        String mchId = config.getStr("mchId", "");
        String apiV3Key = config.getStr("apiV3Key", "");
        String privateKey = config.getStr("privateKey", "");
        String certSerialNo = config.getStr("certSerialNo", "");
        if (!StringUtils.hasText(mchId) || !StringUtils.hasText(apiV3Key) || !StringUtils.hasText(privateKey)) {
            throw new RuntimeException("微信支付配置不完整");
        }
        return new AutoCertificateNotificationConfig.Builder()
                .merchantId(mchId)
                .privateKey(privateKey)
                .merchantSerialNumber(certSerialNo)
                .apiV3Key(apiV3Key)
                .build();
    }
}

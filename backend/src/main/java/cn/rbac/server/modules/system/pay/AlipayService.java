package cn.rbac.server.modules.system.pay;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.rbac.server.modules.system.service.config.SystemConfigHelper;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayService extends AbstractPayService {

    private final SystemConfigHelper configHelper;
    private final PayOrderStore payOrderStore;

    @Override
    public Map<String, String> createTestOrder() {
        Map<String, String> result = new HashMap<>();
        String orderNo = "ALI" + IdUtil.getSnowflakeNextIdStr();
        result.put("orderNo", orderNo);

        JSONObject config = getAlipayConfig();
        if (config == null) {
            throw new RuntimeException("支付宝支付未启用或配置不存在");
        }

        String appId = config.getStr("appId", "");
        String privateKey = config.getStr("privateKey", "");
        String publicKey = config.getStr("publicKey", "");
        String signType = config.getStr("signType", "RSA2");
        String gatewayUrl = config.getStr("gatewayUrl", "https://openapi.alipay.com/gateway.do");
        String notifyUrl = config.getStr("notifyUrl", "");

        if (!StringUtils.hasText(appId) || !StringUtils.hasText(privateKey) || !StringUtils.hasText(publicKey)) {
            throw new RuntimeException("支付宝配置不完整，请检查 AppID、应用私钥、支付宝公钥");
        }

        try {
            AlipayClient alipayClient = buildAlipayClient(config);

            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            request.setNotifyUrl(notifyUrl);
            request.setBizContent("{\"out_trade_no\":\"" + orderNo + "\",\"total_amount\":\"0.01\",\"subject\":\"支付测试订单\"}");

            AlipayTradePrecreateResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                String qrCode = response.getQrCode();
                result.put("qrcode", generateQRCode(qrCode));
                result.put("payUrl", qrCode);
                log.info("支付宝测试订单创建成功: orderNo={}", orderNo);
            } else {
                throw new RuntimeException("支付宝下单失败: " + response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            log.error("创建支付宝测试订单失败", e);
            throw new RuntimeException("创建支付宝测试订单失败: " + e.getErrMsg());
        }
        return result;
    }

    /**
     * 主动向支付宝查单并同步本地订单状态
     */
    public boolean syncPaidStatus(String orderNo) {
        JSONObject config = getAlipayConfig();
        if (config == null) {
            return false;
        }
        try {
            AlipayClient alipayClient = buildAlipayClient(config);
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent("{\"out_trade_no\":\"" + orderNo + "\"}");

            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                log.warn("支付宝查单失败 orderNo={}: {}", orderNo, response.getSubMsg());
                return false;
            }

            String tradeStatus = response.getTradeStatus();
            log.info("支付宝查单: orderNo={}, status={}", orderNo, tradeStatus);
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                payOrderStore.markPaid(orderNo, "alipay", response.getTradeNo(), response.getTotalAmount());
                return true;
            }
        } catch (AlipayApiException e) {
            log.warn("支付宝查单异常 orderNo={}: {}", orderNo, e.getErrMsg());
        }
        return false;
    }

    private AlipayClient buildAlipayClient(JSONObject config) {
        return new DefaultAlipayClient(
                config.getStr("gatewayUrl", "https://openapi.alipay.com/gateway.do"),
                config.getStr("appId", ""),
                config.getStr("privateKey", ""),
                "json",
                "UTF-8",
                config.getStr("publicKey", ""),
                config.getStr("signType", "RSA2"));
    }

    JSONObject getAlipayConfig() {
        JSONObject payment = configHelper.getGroupJson(SystemConfigHelper.GROUP_PAYMENT);
        if (payment == null || !payment.containsKey("alipay")) {
            return null;
        }
        JSONObject config = payment.getJSONObject("alipay");
        if (config == null || !config.getBool("enabled", false)) {
            return null;
        }
        return config;
    }

    @Override
    public String getPayType() {
        return "alipay";
    }

    @Override
    public String getPayTypeName() {
        return "支付宝";
    }
}

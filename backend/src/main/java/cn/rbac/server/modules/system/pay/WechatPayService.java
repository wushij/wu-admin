package cn.rbac.server.modules.system.pay;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.rbac.server.modules.system.service.config.SystemConfigHelper;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.payments.nativepay.model.QueryOrderByOutTradeNoRequest;
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
public class WechatPayService extends AbstractPayService {

    private final SystemConfigHelper configHelper;
    private final PayOrderStore payOrderStore;

    @Override
    public Map<String, String> createTestOrder() {
        Map<String, String> result = new HashMap<>();
        String orderNo = "WX" + IdUtil.getSnowflakeNextIdStr();
        result.put("orderNo", orderNo);

        JSONObject config = getWechatPayConfig();
        if (config == null) {
            throw new RuntimeException("微信支付未启用或配置不存在");
        }

        String mchId = config.getStr("mchId", "");
        String appId = config.getStr("appId", "");
        String apiV3Key = config.getStr("apiV3Key", "");
        String privateKey = config.getStr("privateKey", "");
        String notifyUrl = config.getStr("notifyUrl", "");

        if (!StringUtils.hasText(mchId) || !StringUtils.hasText(appId)
                || !StringUtils.hasText(apiV3Key) || !StringUtils.hasText(privateKey)) {
            throw new RuntimeException("微信支付配置不完整，请检查商户号、AppID、APIv3密钥、商户私钥");
        }

        try {
            NativePayService service = buildNativePayService(config);

            PrepayRequest request = new PrepayRequest();
            request.setAppid(appId);
            request.setMchid(mchId);
            request.setDescription("支付测试订单");
            request.setOutTradeNo(orderNo);
            request.setNotifyUrl(notifyUrl);

            Amount amount = new Amount();
            amount.setTotal(1);
            amount.setCurrency("CNY");
            request.setAmount(amount);

            PrepayResponse response = service.prepay(request);
            String codeUrl = response.getCodeUrl();
            result.put("qrcode", generateQRCode(codeUrl));
            result.put("payUrl", codeUrl);
            log.info("微信支付测试订单创建成功: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("创建微信支付测试订单失败", e);
            throw new RuntimeException("创建微信支付测试订单失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 主动向微信查单并同步本地订单状态（回调不可达时轮询依赖此方法）
     */
    public boolean syncPaidStatus(String orderNo) {
        JSONObject config = getWechatPayConfig();
        if (config == null) {
            return false;
        }
        try {
            NativePayService service = buildNativePayService(config);
            QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
            request.setMchid(config.getStr("mchId", ""));
            request.setOutTradeNo(orderNo);

            Transaction transaction = service.queryOrderByOutTradeNo(request);
            log.info("微信查单: orderNo={}, state={}", orderNo, transaction.getTradeState());

            if (Transaction.TradeStateEnum.SUCCESS.equals(transaction.getTradeState())) {
                String amount = transaction.getAmount() != null
                        ? String.valueOf(transaction.getAmount().getTotal()) : null;
                payOrderStore.markPaid(orderNo, "wechat", transaction.getTransactionId(), amount);
                return true;
            }
        } catch (Exception e) {
            log.warn("微信查单失败 orderNo={}: {}", orderNo, e.getMessage());
        }
        return false;
    }

    private NativePayService buildNativePayService(JSONObject config) {
        String mchId = config.getStr("mchId", "");
        String apiV3Key = config.getStr("apiV3Key", "");
        String privateKey = config.getStr("privateKey", "");
        String certSerialNo = config.getStr("certSerialNo", "");

        Config wechatConfig = new RSAAutoCertificateConfig.Builder()
                .merchantId(mchId)
                .privateKey(privateKey)
                .merchantSerialNumber(certSerialNo)
                .apiV3Key(apiV3Key)
                .build();
        return new NativePayService.Builder().config(wechatConfig).build();
    }

    JSONObject getWechatPayConfig() {
        JSONObject payment = configHelper.getGroupJson(SystemConfigHelper.GROUP_PAYMENT);
        if (payment == null || !payment.containsKey("wechatPay")) {
            return null;
        }
        JSONObject config = payment.getJSONObject("wechatPay");
        if (config == null || !config.getBool("enabled", false)) {
            return null;
        }
        return config;
    }

    @Override
    public String getPayType() {
        return "wechat";
    }

    @Override
    public String getPayTypeName() {
        return "微信支付";
    }
}

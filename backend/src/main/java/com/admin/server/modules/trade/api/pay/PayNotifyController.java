package com.admin.server.modules.trade.api.pay;

import com.admin.server.modules.trade.framework.pay.PayNotifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付异步回调（微信 / 支付宝，无需登录）
 */
@Tag(name = "支付回调")
@RestController
@RequiredArgsConstructor
public class PayNotifyController {

    private final PayNotifyService payNotifyService;

    @Operation(summary = "微信支付异步通知")
    @PostMapping("/pay/notify/wechat")
    public Map<String, String> wechatNotify(HttpServletRequest request, @RequestBody String body) {
        return payNotifyService.handleWechatNotify(
                request.getHeader("Wechatpay-Serial"),
                request.getHeader("Wechatpay-Nonce"),
                request.getHeader("Wechatpay-Timestamp"),
                request.getHeader("Wechatpay-Signature"),
                body);
    }

    @Operation(summary = "支付宝异步通知")
    @PostMapping("/pay/notify/alipay")
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return payNotifyService.handleAlipayNotify(params);
    }
}

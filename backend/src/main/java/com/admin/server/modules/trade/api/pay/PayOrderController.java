package com.admin.server.modules.trade.api.pay;

import com.admin.server.common.exception.BusinessException;
import com.admin.server.common.core.CommonResult;
import com.admin.server.modules.trade.framework.pay.PayNotifyService;
import com.admin.server.modules.trade.framework.pay.PayOrderRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "支付订单")
@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class PayOrderController {

    private final PayNotifyService payNotifyService;

    @Operation(summary = "查询测试订单支付状态（含主动向支付平台查单）")
    @GetMapping("/order/{orderNo}")
    @PreAuthorize("@ss.hasRead('system:config:list')")
    public CommonResult<PayOrderRecord> getOrderStatus(@PathVariable String orderNo) {
        PayOrderRecord record = payNotifyService.syncOrderStatus(orderNo);
        if (record == null) {
            throw new BusinessException(404, "订单不存在");
        }
        return CommonResult.success(record);
    }
}

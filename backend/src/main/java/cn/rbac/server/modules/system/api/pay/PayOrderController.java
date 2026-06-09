package cn.rbac.server.modules.system.api.pay;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.modules.system.pay.PayNotifyService;
import cn.rbac.server.modules.system.pay.PayOrderRecord;
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

package com.admin.server.modules.trade.framework.pay;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PayOrderRecord {

    private String orderNo;
    private String payType;
    /** PENDING / PAID */
    private String status;
    private String transactionId;
    private String amount;
    private LocalDateTime createTime;
    private LocalDateTime paidTime;
}

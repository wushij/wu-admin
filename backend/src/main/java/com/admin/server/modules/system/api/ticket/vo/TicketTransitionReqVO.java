package com.admin.server.modules.system.api.ticket.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TicketTransitionReqVO {
    @NotNull(message = "工单ID不能为空")
    private Long id;
    @NotBlank(message = "目标状态不能为空")
    private String status;
}

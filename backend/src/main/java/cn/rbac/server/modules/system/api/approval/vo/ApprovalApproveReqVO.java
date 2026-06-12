package cn.rbac.server.modules.system.api.approval.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalApproveReqVO {
    @NotNull(message = "审批单ID不能为空")
    private Long id;
    @NotBlank(message = "审批动作不能为空")
    private String action;
    private String remark;
}

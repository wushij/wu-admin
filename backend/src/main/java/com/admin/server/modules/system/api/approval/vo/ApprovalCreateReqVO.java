package com.admin.server.modules.system.api.approval.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApprovalCreateReqVO {
    private String formType;
    @NotBlank(message = "审批单标题不能为空")
    @Size(max = 100, message = "标题最多 100 个字符")
    private String title;
    private String content;
    @NotNull(message = "请选择审批人")
    private Long approverUserId;
}

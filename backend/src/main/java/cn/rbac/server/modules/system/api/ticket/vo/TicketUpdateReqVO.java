package cn.rbac.server.modules.system.api.ticket.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TicketUpdateReqVO {
    @NotNull(message = "工单ID不能为空")
    private Long id;
    @NotBlank(message = "工单标题不能为空")
    @Size(max = 100, message = "标题最多 100 个字符")
    private String title;
    private String description;
    private String priority;
    private Long assigneeUserId;
    private LocalDateTime deadline;
}

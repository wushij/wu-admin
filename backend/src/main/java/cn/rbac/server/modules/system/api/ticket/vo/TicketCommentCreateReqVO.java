package cn.rbac.server.modules.system.api.ticket.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TicketCommentCreateReqVO {
    @NotNull(message = "工单ID不能为空")
    private Long ticketId;
    @NotBlank(message = "评论内容不能为空")
    private String content;
}

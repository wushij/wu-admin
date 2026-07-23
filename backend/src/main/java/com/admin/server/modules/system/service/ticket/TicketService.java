package com.admin.server.modules.system.service.ticket;

import com.admin.server.common.pojo.PageParam;
import com.admin.server.common.pojo.PageResult;
import com.admin.server.modules.system.api.ticket.vo.*;
import com.admin.server.modules.system.dal.dataobject.ticket.TicketAttachmentDO;
import com.admin.server.modules.system.dal.dataobject.ticket.TicketCommentDO;
import com.admin.server.modules.system.dal.dataobject.ticket.TicketDO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface TicketService {

    List<AssigneeOptionVO> getAssigneeOptions(Long currentUserId);

    PageResult<TicketDO> page(PageParam pageParam, String title, String status,
                               String priority, Long assigneeUserId, Long currentUserId);

    TicketDO getDetail(Long id);

    Long create(TicketCreateReqVO reqVO, Long currentUserId);

    void update(TicketUpdateReqVO reqVO, Long currentUserId);

    void delete(Long id);

    PageResult<TicketDO> recyclePage(PageParam pageParam, String title, String status, String priority);

    void restore(Long id);

    void deletePermanent(Long id);

    void transition(TicketTransitionReqVO reqVO, Long currentUserId);

    List<TicketCommentDO> listComments(Long ticketId);

    Long createComment(TicketCommentCreateReqVO reqVO, Long currentUserId);

    List<TicketAttachmentDO> listAttachments(Long ticketId);

    Long uploadAttachment(Long ticketId, MultipartFile file, Long currentUserId) throws IOException;

    TicketAttachmentDO getAttachmentForDownload(Long id);
}

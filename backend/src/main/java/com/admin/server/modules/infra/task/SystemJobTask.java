package com.admin.server.modules.infra.task;

import com.admin.server.modules.message.dal.mysql.ChatGroupMessageMapper;
import com.admin.server.modules.message.dal.mysql.ChatMessageMapper;
import com.admin.server.modules.system.dal.mysql.notice.NoticeMapper;
import com.admin.server.modules.ticket.dal.mysql.ticket.TicketAttachmentMapper;
import com.admin.server.modules.ticket.dal.mysql.ticket.TicketCommentMapper;
import com.admin.server.modules.ticket.dal.mysql.ticket.TicketMapper;
import com.admin.server.modules.system.framework.cache.DictCacheService;
import com.admin.server.modules.system.framework.cache.SysConfigCacheService;
import com.admin.server.modules.infra.service.file.SysFileService;
import com.admin.server.modules.infra.service.job.SysJobLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component("systemJobTask")
public class SystemJobTask {

    @Value("${app.job.log-retention-days:30}")
    private int jobLogRetentionDays;

    @Value("${app.job.notice-read-retention-days:90}")
    private int noticeReadRetentionDays;

    @Value("${app.job.ticket-recycle-retention-days:30}")
    private int ticketRecycleRetentionDays;

    @Value("${app.job.chat-message-retention-days:180}")
    private int chatMessageRetentionDays;

    @Value("${app.job.group-chat-message-retention-days:180}")
    private int groupChatMessageRetentionDays;

    @Resource
    private LogRetentionTask logRetentionTask;
    @Resource
    private DictCacheService dictCacheService;
    @Resource
    private SysConfigCacheService sysConfigCacheService;
    @Resource
    private SysJobLogService jobLogService;
    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private TicketMapper ticketMapper;
    @Resource
    private TicketCommentMapper ticketCommentMapper;
    @Resource
    private TicketAttachmentMapper ticketAttachmentMapper;
    @Resource
    private SysFileService fileService;
    @Resource
    private ChatMessageMapper chatMessageMapper;
    @Resource
    private ChatGroupMessageMapper chatGroupMessageMapper;

    /** 清理过期操作/登录/API 访问日志（保留天数见 application.yml） */
    public void purgeExpiredLogs() {
        log.info("开始执行过期日志归档清理...");
        logRetentionTask.purgeExpiredLogs();
    }

    /** 清理超过保留期的私聊消息 */
    public void purgeOldChatMessages() {
        if (chatMessageRetentionDays <= 0) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(chatMessageRetentionDays);
        int removed = chatMessageMapper.deleteOlderThan(cutoff);
        if (removed > 0) {
            log.info("私聊消息清理：删除 {} 条（早于 {}）", removed, cutoff);
        }
    }

    /** 清理超过保留期的群聊消息 */
    public void purgeOldGroupChatMessages() {
        if (groupChatMessageRetentionDays <= 0) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(groupChatMessageRetentionDays);
        int removed = chatGroupMessageMapper.deleteOlderThan(cutoff);
        if (removed > 0) {
            log.info("群聊消息清理：删除 {} 条（早于 {}）", removed, cutoff);
        }
    }

    /** 全量刷新字典 Redis 缓存（保留供手动任务调用） */
    public void refreshDictCache() {
        log.info("开始刷新字典缓存...");
        dictCacheService.refreshAll();
        log.info("字典缓存刷新完成");
    }

    /** 全量刷新系统配置 Redis 缓存（保留供手动任务调用） */
    public void refreshConfigCache() {
        log.info("开始刷新系统配置缓存...");
        sysConfigCacheService.refreshAll();
        log.info("系统配置缓存刷新完成");
    }

    /** 清理超保留期的调度执行日志 */
    public void purgeExpiredJobLogs() {
        log.info("开始清理 {} 天前的调度日志...", jobLogRetentionDays);
        int removed = jobLogService.cleanOlderThan(jobLogRetentionDays);
        log.info("调度日志清理完成，删除 {} 条", removed);
    }

    /** 清理已读且超过保留期的站内通知 */
    public void purgeReadNotices() {
        if (noticeReadRetentionDays <= 0) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(noticeReadRetentionDays);
        int removed = noticeMapper.deleteReadOlderThan(cutoff);
        if (removed > 0) {
            log.info("已读通知清理：删除 {} 条（早于 {}）", removed, cutoff);
        }
    }

    /** 彻底删除回收站中超过保留期的工单及其评论、附件 */
    public void purgeTicketRecycleBin() {
        if (ticketRecycleRetentionDays <= 0) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(ticketRecycleRetentionDays);
        List<Long> ids = ticketMapper.selectExpiredRecycleIds(cutoff);
        if (ids.isEmpty()) {
            return;
        }
        int removed = 0;
        for (Long id : ids) {
            ticketCommentMapper.deletePhysicalByTicketId(id);
            ticketAttachmentMapper.deletePhysicalByTicketId(id);
            removed += ticketMapper.deletePhysicalById(id);
        }
        log.info("工单回收站清理：彻底删除 {} 条（早于 {}）", removed, cutoff);
    }

    /** 彻底删除回收站中超过保留期的文件记录及磁盘文件 */
    public void purgeFileRecycleBin() {
        fileService.purgeExpiredRecycleBin();
    }
}

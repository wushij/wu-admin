package com.admin.server.modules.system.service.message;

import com.admin.server.common.pojo.BusinessException;
import com.admin.server.common.pojo.PageParam;
import com.admin.server.common.pojo.PageResult;
import com.admin.server.framework.websocket.MessageWebSocketHandler;
import com.admin.server.framework.security.core.service.SecurityUtils;
import com.admin.server.modules.system.dal.dataobject.message.AnnounceDO;
import com.admin.server.modules.system.dal.dataobject.message.AnnounceSendLogDO;
import com.admin.server.modules.system.dal.dataobject.message.UserAnnounceDO;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;
import com.admin.server.modules.system.dal.mysql.message.AnnounceMapper;
import com.admin.server.modules.system.dal.mysql.message.AnnounceSendLogMapper;
import com.admin.server.modules.system.dal.mysql.message.UserAnnounceMapper;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.admin.server.modules.system.service.message.vo.AnnounceMyVO;

@Service
public class AnnounceService {

    @Resource
    private AnnounceMapper announceMapper;
    @Resource
    private UserAnnounceMapper userAnnounceMapper;
    @Resource
    private AnnounceSendLogMapper sendLogMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private MessageWebSocketHandler webSocketHandler;
    @Resource
    private ObjectMapper objectMapper;

    public Page<AnnounceDO> page(int pageNo, int pageSize, String title, Integer noticeType, Integer status) {
        LambdaQueryWrapper<AnnounceDO> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(title)) {
            w.like(AnnounceDO::getTitle, title);
        }
        if (noticeType != null) {
            w.eq(AnnounceDO::getNoticeType, noticeType);
        }
        if (status != null) {
            w.eq(AnnounceDO::getStatus, status);
        }
        w.orderByDesc(AnnounceDO::getCreateTime);
        return announceMapper.selectPage(new Page<>(pageNo, pageSize), w);
    }

    public Page<AnnounceMyVO> myPage(Long userId, int pageNo, int pageSize, Integer isRead) {
        LambdaQueryWrapper<UserAnnounceDO> uw = new LambdaQueryWrapper<UserAnnounceDO>()
                .eq(UserAnnounceDO::getUserId, userId);
        if (isRead != null) {
            uw.eq(UserAnnounceDO::getIsRead, isRead);
        }
        uw.orderByDesc(UserAnnounceDO::getCreateTime);
        Page<UserAnnounceDO> uaPage = userAnnounceMapper.selectPage(new Page<>(pageNo, pageSize), uw);
        List<AnnounceMyVO> records = new ArrayList<>();
        for (UserAnnounceDO ua : uaPage.getRecords()) {
            AnnounceDO a = announceMapper.selectById(ua.getAnnounceId());
            if (a != null && a.getStatus() != null && a.getStatus() == 1) {
        AnnounceMyVO vo = new AnnounceMyVO();
                vo.setId(a.getId());
                vo.setTitle(a.getTitle());
                vo.setContent(a.getContent());
                vo.setNoticeType(a.getNoticeType());
                vo.setStatus(a.getStatus());
                vo.setCreateName(resolvePublisherName(a));
                vo.setCreateTime(a.getCreateTime());
                vo.setIsRead(ua.getIsRead());
                vo.setReadTime(ua.getReadTime());
                records.add(vo);
            }
        }
        Page<AnnounceMyVO> result = new Page<>(pageNo, pageSize, uaPage.getTotal());
        result.setRecords(records);
        return result;
    }

    public AnnounceDO getById(Long id) {
        return announceMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(AnnounceDO entity) {
        Long uid = SecurityUtils.getLoginUserId();
        entity.setCreateBy(uid);
        UserDO user = userMapper.selectById(uid);
        if (user != null) {
            entity.setCreateName(user.getNickname());
        }
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        if (!StringUtils.hasText(entity.getChannels())) {
            entity.setChannels("[\"station\"]");
        }
        announceMapper.insert(entity);
        if (entity.getStatus() != null && entity.getStatus() == 1) {
            publish(entity.getId());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(AnnounceDO entity) {
        announceMapper.updateById(entity);
        if (entity.getStatus() != null && entity.getStatus() == 1) {
            publish(entity.getId());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (announceMapper.selectById(id) == null) {
            throw new BusinessException(404, "通知不存在");
        }
        announceMapper.deleteById(id);
    }

    public PageResult<AnnounceDO> recyclePage(PageParam pageParam, String title) {
        Page<AnnounceDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<AnnounceDO> deletedPage = (Page<AnnounceDO>) announceMapper.selectDeletedPage(page, title);
        return PageResult.of(deletedPage.getRecords(), deletedPage.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        int rows = announceMapper.restoreById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站通知不存在");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletePermanent(Long id) {
        int rows = announceMapper.deletePhysicalById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站通知不存在");
        }
        userAnnounceMapper.deletePhysicalByAnnounceId(id);
        sendLogMapper.deletePhysicalByAnnounceId(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        AnnounceDO announce = announceMapper.selectById(id);
        if (announce == null) {
            throw new BusinessException(404, "通知不存在");
        }
        announce.setStatus(1);
        announceMapper.updateById(announce);
        List<Long> userIds = resolveTargetUserIds(announce);
        Set<Long> existing = new HashSet<>(userAnnounceMapper.selectUserIdsByAnnounceId(id));
        List<UserAnnounceDO> toInsert = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Long uid : userIds) {
            if (uid == null || uid <= 0) {
                continue;
            }
            if (!existing.contains(uid)) {
                UserAnnounceDO ua = new UserAnnounceDO();
                ua.setUserId(uid);
                ua.setAnnounceId(id);
                ua.setIsRead(0);
                ua.setCreateTime(now);
                toInsert.add(ua);
            }
        }
        if (!toInsert.isEmpty()) {
            userAnnounceMapper.insertBatch(toInsert);
        }
        AnnounceSendLogDO log = new AnnounceSendLogDO();
        log.setAnnounceId(id);
        log.setChannel("station");
        log.setStatus(1);
        log.setTargetCount(userIds.size());
        log.setSuccessCount(userIds.size());
        log.setSendTime(LocalDateTime.now());
        sendLogMapper.insert(log);

        final String title = announce.getTitle();
        final String content = announce.getContent();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (Long uid : userIds) {
                    if (uid == null || uid <= 0) {
                        continue;
                    }
                    webSocketHandler.sendNotice(uid, id, title, content);
                }
            }
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long announceId) {
        userAnnounceMapper.update(null, new LambdaUpdateWrapper<UserAnnounceDO>()
                .eq(UserAnnounceDO::getUserId, userId)
                .eq(UserAnnounceDO::getAnnounceId, announceId)
                .set(UserAnnounceDO::getIsRead, 1)
                .set(UserAnnounceDO::getReadTime, LocalDateTime.now()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId) {
        userAnnounceMapper.update(null, new LambdaUpdateWrapper<UserAnnounceDO>()
                .eq(UserAnnounceDO::getUserId, userId)
                .eq(UserAnnounceDO::getIsRead, 0)
                .set(UserAnnounceDO::getIsRead, 1)
                .set(UserAnnounceDO::getReadTime, LocalDateTime.now()));
    }

    public long unreadCount(Long userId) {
        return userAnnounceMapper.countUnread(userId);
    }

    public List<AnnounceSendLogDO> sendLogs(Long announceId) {
        return sendLogMapper.selectList(new LambdaQueryWrapper<AnnounceSendLogDO>()
                .eq(AnnounceSendLogDO::getAnnounceId, announceId)
                .orderByDesc(AnnounceSendLogDO::getSendTime));
    }

    private List<Long> resolveTargetUserIds(AnnounceDO announce) {
        Integer targetType = announce.getTargetType() == null ? 3 : announce.getTargetType();
        if (targetType == 3) {
            return userMapper.selectList(new LambdaQueryWrapper<UserDO>().eq(UserDO::getStatus, 1))
                    .stream().map(UserDO::getId).collect(Collectors.toList());
        }
        List<Long> ids = parseTargetIds(announce.getTargetIds());
        if (targetType == 1) {
            return ids;
        }
        if (targetType == 2) {
            Set<Long> set = new HashSet<>();
            for (Long deptId : ids) {
                userMapper.selectList(new LambdaQueryWrapper<UserDO>()
                        .eq(UserDO::getDeptId, deptId)
                        .eq(UserDO::getStatus, 1))
                        .forEach(u -> set.add(u.getId()));
            }
            return new ArrayList<>(set);
        }
        return List.of();
    }

    private List<Long> parseTargetIds(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    /** 优先返回发布人当前昵称/用户名，避免资料变更后仍显示旧快照名 */
    public String resolvePublisherName(AnnounceDO announce) {
        if (announce == null) {
            return null;
        }
        UserDO publisher = resolvePublisher(announce);
        if (publisher != null) {
            if (StringUtils.hasText(publisher.getNickname())) {
                return publisher.getNickname();
            }
            if (StringUtils.hasText(publisher.getUsername())) {
                return publisher.getUsername();
            }
        }
        return announce.getCreateName();
    }

    /** 返回发布人当前头像；资料变更后不再显示旧快照 */
    public String resolvePublisherAvatar(AnnounceDO announce) {
        UserDO publisher = resolvePublisher(announce);
        return publisher != null ? publisher.getAvatar() : null;
    }

    /** 按 createBy 解析发布人实体，供昵称/头像等派生字段复用，避免 Controller 直接操作 Mapper */
    private UserDO resolvePublisher(AnnounceDO announce) {
        if (announce == null || announce.getCreateBy() == null) {
            return null;
        }
        return userMapper.selectById(announce.getCreateBy());
    }
}

package cn.rbac.server.modules.system.service.message;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.framework.websocket.MessageWebSocketHandler;
import cn.rbac.server.framework.security.core.service.SecurityUtils;
import cn.rbac.server.modules.system.dal.dataobject.message.AnnounceDO;
import cn.rbac.server.modules.system.dal.dataobject.message.AnnounceSendLogDO;
import cn.rbac.server.modules.system.dal.dataobject.message.UserAnnounceDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.dal.mysql.message.AnnounceMapper;
import cn.rbac.server.modules.system.dal.mysql.message.AnnounceSendLogMapper;
import cn.rbac.server.modules.system.dal.mysql.message.UserAnnounceMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import cn.rbac.server.modules.system.service.message.vo.AnnounceMyVO;

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
                vo.setCreateName(a.getCreateName());
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
            throw new IllegalArgumentException("通知不存在");
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
            throw new IllegalArgumentException("通知不存在");
        }
        announce.setStatus(1);
        announceMapper.updateById(announce);
        List<Long> userIds = resolveTargetUserIds(announce);
        int success = 0;
        for (Long uid : userIds) {
            UserAnnounceDO exist = userAnnounceMapper.selectOne(new LambdaQueryWrapper<UserAnnounceDO>()
                    .eq(UserAnnounceDO::getUserId, uid)
                    .eq(UserAnnounceDO::getAnnounceId, id));
            if (exist == null) {
                UserAnnounceDO ua = new UserAnnounceDO();
                ua.setUserId(uid);
                ua.setAnnounceId(id);
                ua.setIsRead(0);
                ua.setCreateTime(LocalDateTime.now());
                userAnnounceMapper.insert(ua);
            }
            webSocketHandler.sendNotice(uid, id, announce.getTitle(), announce.getContent());
            success++;
        }
        AnnounceSendLogDO log = new AnnounceSendLogDO();
        log.setAnnounceId(id);
        log.setChannel("station");
        log.setStatus(1);
        log.setTargetCount(userIds.size());
        log.setSuccessCount(success);
        log.setSendTime(LocalDateTime.now());
        sendLogMapper.insert(log);
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
}

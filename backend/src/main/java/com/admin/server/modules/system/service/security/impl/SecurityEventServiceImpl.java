package com.admin.server.modules.system.service.security.impl;

import cn.hutool.core.util.StrUtil;
import com.admin.server.modules.system.api.security.vo.RiskSubjectVO;
import com.admin.server.modules.system.dal.dataobject.security.SecurityEventDO;
import com.admin.server.modules.system.dal.mysql.security.SecurityEventMapper;
import com.admin.server.modules.system.service.security.AntiBotRiskService;
import com.admin.server.modules.system.service.security.SecurityEventService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 安全事件审计日志服务实现
 */
@Slf4j
@Service
public class SecurityEventServiceImpl implements SecurityEventService {

    @Resource
    private SecurityEventMapper securityEventMapper;

    @Lazy
    @Resource
    private AntiBotRiskService antiBotRiskService;

    @Override
    public Page<SecurityEventDO> getPage(Integer pageNo, Integer pageSize, String ipaddr, String eventType, String action) {
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 500);
        Page<SecurityEventDO> page = new Page<>(safePageNo, safePageSize);
        LambdaQueryWrapper<SecurityEventDO> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(ipaddr)) {
            wrapper.like(SecurityEventDO::getIpaddr, ipaddr.trim());
        }
        if (StrUtil.isNotBlank(eventType)) {
            wrapper.eq(SecurityEventDO::getEventType, eventType.trim());
        }
        if (StrUtil.isNotBlank(action)) {
            wrapper.eq(SecurityEventDO::getAction, action.trim());
        }
        wrapper.orderByDesc(SecurityEventDO::getCreateTime);
        return securityEventMapper.selectPage(page, wrapper);
    }

    @Async
    @Override
    public void recordAsync(SecurityEventDO event) {
        if (event == null) return;
        try {
            if (event.getCreateTime() == null) {
                event.setCreateTime(LocalDateTime.now());
            }
            securityEventMapper.insert(event);
        } catch (Exception e) {
            log.error("异步落库安全事件失败: ip={}, type={}", event.getIpaddr(), event.getEventType(), e);
        }
    }

    @Override
    public List<SecurityEventDO> getRecentByPrincipal(String principal, int limit) {
        if (StrUtil.isBlank(principal)) {
            return java.util.Collections.emptyList();
        }
        int safeLimit = Math.max(1, Math.min(limit, 200));
        LambdaQueryWrapper<SecurityEventDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecurityEventDO::getPrincipal, principal.trim());
        wrapper.orderByDesc(SecurityEventDO::getCreateTime);
        wrapper.last("LIMIT " + safeLimit);
        return securityEventMapper.selectList(wrapper);
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        // 今日拦截事件总数
        Long todayTotal = securityEventMapper.selectCount(new LambdaQueryWrapper<SecurityEventDO>()
                .ge(SecurityEventDO::getCreateTime, todayStart));
        stats.put("todayTotal", todayTotal != null ? todayTotal : 0L);

        // 今日直接阻断次数 (BLOCKED)
        Long todayBlocked = securityEventMapper.selectCount(new LambdaQueryWrapper<SecurityEventDO>()
                .ge(SecurityEventDO::getCreateTime, todayStart)
                .eq(SecurityEventDO::getAction, "BLOCKED"));
        stats.put("todayBlocked", todayBlocked != null ? todayBlocked : 0L);

        // 今日自动封禁次数：跃迁到封禁的事件 action 会被改写成 FORBIDDEN，
        // 与「直接拒绝」分开统计，否则大盘会少算真实封禁量。
        Long todayForbidden = securityEventMapper.selectCount(new LambdaQueryWrapper<SecurityEventDO>()
                .ge(SecurityEventDO::getCreateTime, todayStart)
                .eq(SecurityEventDO::getAction, "FORBIDDEN"));
        stats.put("todayForbidden", todayForbidden != null ? todayForbidden : 0L);

        // 今日警告与挑战次数
        Long todayWarning = securityEventMapper.selectCount(new LambdaQueryWrapper<SecurityEventDO>()
                .ge(SecurityEventDO::getCreateTime, todayStart)
                .eq(SecurityEventDO::getAction, "WARNING"));
        stats.put("todayWarning", todayWarning != null ? todayWarning : 0L);

        Long todayChallenge = securityEventMapper.selectCount(new LambdaQueryWrapper<SecurityEventDO>()
                .ge(SecurityEventDO::getCreateTime, todayStart)
                .eq(SecurityEventDO::getAction, "CHALLENGE"));
        stats.put("todayChallenge", todayChallenge != null ? todayChallenge : 0L);

        // 今日爬虫 UA 拦截次数
        Long todayUaBlock = securityEventMapper.selectCount(new LambdaQueryWrapper<SecurityEventDO>()
                .ge(SecurityEventDO::getCreateTime, todayStart)
                .eq(SecurityEventDO::getEventType, "UA_BLOCK"));
        stats.put("todayUaBlock", todayUaBlock != null ? todayUaBlock : 0L);

        // 今日重放攻击拦截次数
        Long todayReplayBlock = securityEventMapper.selectCount(new LambdaQueryWrapper<SecurityEventDO>()
                .ge(SecurityEventDO::getCreateTime, todayStart)
                .eq(SecurityEventDO::getEventType, "ANTI_REPLAY"));
        stats.put("todayReplayBlock", todayReplayBlock != null ? todayReplayBlock : 0L);

        // 今日蜜罐诱捕命中次数
        Long todayHoneypot = securityEventMapper.selectCount(new LambdaQueryWrapper<SecurityEventDO>()
                .ge(SecurityEventDO::getCreateTime, todayStart)
                .eq(SecurityEventDO::getEventType, "HONEYPOT"));
        stats.put("todayHoneypot", todayHoneypot != null ? todayHoneypot : 0L);

        // 今日越权探测次数
        Long todayPermissionDenied = securityEventMapper.selectCount(new LambdaQueryWrapper<SecurityEventDO>()
                .ge(SecurityEventDO::getCreateTime, todayStart)
                .eq(SecurityEventDO::getEventType, "PERMISSION_DENIED"));
        stats.put("todayPermissionDenied", todayPermissionDenied != null ? todayPermissionDenied : 0L);

        // 活跃风险主体：一次读取同时产出封禁数、等级分布和 Top 列表，避免重复扫描 Redis
        List<RiskSubjectVO> subjects;
        try {
            subjects = antiBotRiskService.getActiveRiskList();
        } catch (Exception e) {
            subjects = java.util.Collections.emptyList();
        }
        int activeBlockedCount = 0;
        Map<String, Long> levelDistribution = new LinkedHashMap<>();
        for (RiskSubjectVO vo : subjects) {
            if (Boolean.TRUE.equals(vo.getBlocked())) {
                activeBlockedCount++;
            }
            String levelText = vo.getRiskLevelText() == null ? "未知" : vo.getRiskLevelText();
            levelDistribution.merge(levelText, 1L, Long::sum);
        }
        stats.put("activeBlockedCount", activeBlockedCount);
        stats.put("activeRiskCount", subjects.size());
        stats.put("riskLevelDistribution", levelDistribution);
        stats.put("topRiskSubjects", subjects.stream().limit(5).toList());

        return stats;
    }

    @Override
    public void delete(Long id) {
        securityEventMapper.deleteById(id);
    }

    @Override
    public void clear() {
        securityEventMapper.delete(new LambdaQueryWrapper<>());
    }
}

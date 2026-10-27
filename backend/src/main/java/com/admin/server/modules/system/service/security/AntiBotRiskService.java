package com.admin.server.modules.system.service.security;

import com.admin.server.framework.security.core.RiskPrincipal;
import com.admin.server.framework.security.core.RiskScoringApi;
import com.admin.server.modules.system.api.security.vo.RiskSubjectVO;
import com.admin.server.modules.system.dal.dataobject.security.SecurityEventDO;

import java.util.List;

/**
 * 行为风控与反爬防御服务。
 *
 * <p>计分模型：风险分按主体（账号 / IP）落在 Redis 的有序集合里，
 * 每个事件是一个成员、分值写在成员内容中、时间戳作为 score。
 * 读取时只汇总最近一个滑动窗口内的事件，超过窗口的分值自然失效，
 * 因此不需要额外的"衰减任务"，也不会出现分数只增不减导致的永久黑名单。</p>
 */
public interface AntiBotRiskService extends RiskScoringApi {

    /**
     * 兼容旧签名的计分入口。
     *
     * <p>蜜罐等既有调用方传入的是裸 IP，内部会统一归一化为 {@code ip:} 主体。</p>
     */
    int addRisk(String principal, String ip, int delta, String eventType, String action,
                String uri, String method, String userAgent, String reason);

    /**
     * 按风险主体计分并执行处置跃迁。
     *
     * @return 处置后该主体的累计风险分
     */
    int addRisk(RiskPrincipal subject, int delta, String eventType, String action,
                String uri, String method, String userAgent, String reason);

    /**
     * 读取主体当前风险分（滑动窗口内）。
     */
    int getRiskScore(String principal);

    /**
     * 读取主体当前风险等级。
     */
    RiskLevel getRiskLevel(String principal);

    /**
     * 判断 IP 是否处于封禁状态。
     */
    boolean isBlocked(String ip);

    /**
     * 判断主体是否处于封禁状态。
     */
    boolean isBlocked(RiskPrincipal subject);

    /**
     * 手动或自动封禁主体。
     */
    void blockPrincipal(String principal, long durationSeconds, String reason);

    /**
     * 解除主体封禁。
     *
     * @param resetRisk 是否同时清零风险分
     */
    void unblockPrincipal(String principal, boolean resetRisk);

    /**
     * 解除主体封禁并清零风险分（兼容旧调用）。
     */
    default void unblockPrincipal(String principal) {
        unblockPrincipal(principal, true);
    }

    /**
     * 当前处于封禁或存在风险分的全部主体。
     */
    List<RiskSubjectVO> getActiveRiskList();

    /**
     * 风险主体分页查询。
     */
    RiskSubjectPage getRiskSubjectPage(int pageNo, int pageSize, String keyword, Integer level, Boolean blocked);

    /**
     * 查询某个主体最近的安全事件。
     */
    List<SecurityEventDO> getRecentEvents(String principal, int limit);

    /**
     * 匹配爬虫/自动化工具 UA 指纹。
     *
     * @return 命中的指纹，未命中返回 {@code null}
     */
    String matchBlockedUserAgent(String userAgent);

    /**
     * 当前主体是否需要走二次验证（挑战）。
     */
    boolean isChallengeRequired(RiskPrincipal subject);

    /**
     * 标记主体已通过一次风控挑战，短时间内不再重复要求。
     */
    void markChallengePassed(RiskPrincipal subject);
}

package com.admin.server.framework.security.core;

/**
 * 风控打分与拦截服务 SPI（框架层定义，业务层 AntiBotRiskService 实现）
 */
public interface RiskScoringApi {

    /**
     * 判断 IP 是否处于封禁状态
     */
    boolean isBlocked(String ip);

    /**
     * 判断风险主体是否处于封禁状态
     */
    boolean isBlocked(RiskPrincipal subject);

    /**
     * 匹配是否为已阻断的爬虫 User-Agent 特征
     *
     * @return 匹配到的指纹特征，未命中返回 null
     */
    String matchBlockedUserAgent(String userAgent);

    /**
     * 为风险主体累加风险分并记录事件
     */
    int addRisk(RiskPrincipal subject, int delta, String eventType, String action,
                String uri, String method, String userAgent, String detail);

    /**
     * 判断主体是否需要完成风控人机挑战
     */
    boolean isChallengeRequired(RiskPrincipal subject);

    /**
     * 标记主体已通过风控挑战
     */
    void markChallengePassed(RiskPrincipal subject);
}

package com.admin.server.modules.system.service.security;

/**
 * 风险等级（0～5）。
 *
 * <p>等级是"处置档位"而不是单纯的分值区间：不同等级对应不同的动作，
 * 便于过滤器、告警和运营面板使用同一套语义。阈值来自系统配置，
 * 默认 30 / 50 / 70 / 90 / 120，临时封禁与最高档封禁阈值分别由 {@code riskBlockScore}、{@code riskBanScore} 决定。</p>
 */
public enum RiskLevel {

    /** 正常：不记录、不干预 */
    NORMAL(0, "正常"),

    /** 关注：只记录事件，便于事后回溯 */
    NOTICE(1, "关注"),

    /** 警告：记录并推送告警，不阻断请求 */
    WARNING(2, "警告"),

    /** 挑战：可按配置对写操作要求二次验证 */
    CHALLENGE(3, "挑战"),

    /** 临时封禁：写入账号/主体封禁标记，后续请求直接拒绝，不连带封 IP */
    BLOCK(4, "临时封禁"),

    /** 最高档封禁：封禁主体并连带封 IP，时长不短于临时封禁 */
    BAN(5, "最高档封禁");

    private final int code;
    private final String text;

    RiskLevel(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    /**
     * 根据分数与四档阈值计算等级（不区分最高档封禁，保持向后兼容）。
     *
     * @param score         当前风险分
     * @param noticeScore   关注阈值
     * @param warningScore  警告阈值
     * @param challengeScore 挑战阈值
     * @param blockScore    临时封禁阈值
     */
    public static RiskLevel of(int score, int noticeScore, int warningScore, int challengeScore, int blockScore) {
        if (score >= blockScore) {
            return BLOCK;
        }
        if (score >= challengeScore) {
            return CHALLENGE;
        }
        if (score >= warningScore) {
            return WARNING;
        }
        if (score >= noticeScore) {
            return NOTICE;
        }
        return NORMAL;
    }

    /**
     * 根据分数与五档阈值计算等级，临时封禁与最高档封禁分别对应 {@link #BLOCK} 与 {@link #BAN}。
     *
     * @param banScore 最高档封禁阈值
     */
    public static RiskLevel of(int score, int noticeScore, int warningScore, int challengeScore,
                               int blockScore, int banScore) {
        if (score >= banScore) {
            return BAN;
        }
        return of(score, noticeScore, warningScore, challengeScore, blockScore);
    }

    public static RiskLevel fromCode(int code) {
        for (RiskLevel level : values()) {
            if (level.code == code) {
                return level;
            }
        }
        return NORMAL;
    }
}
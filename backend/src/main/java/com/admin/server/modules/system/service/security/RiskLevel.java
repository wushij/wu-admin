package com.admin.server.modules.system.service.security;

/**
 * 风险等级（0～4）。
 *
 * <p>等级是"处置档位"而不是单纯的分值区间：不同等级对应不同的动作，
 * 便于过滤器、告警和运营面板使用同一套语义。阈值来自系统配置，
 * 默认 30 / 50 / 70 / 90，封禁阈值随 {@code riskBlockScore} 变化。</p>
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

    /** 封禁：写入封禁标记，后续请求直接拒绝 */
    BLOCK(4, "封禁");

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
     * 根据分数与阈值计算等级。
     *
     * @param score         当前风险分
     * @param noticeScore   关注阈值
     * @param warningScore  警告阈值
     * @param challengeScore 挑战阈值
     * @param blockScore    封禁阈值
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

    public static RiskLevel fromCode(int code) {
        for (RiskLevel level : values()) {
            if (level.code == code) {
                return level;
            }
        }
        return NORMAL;
    }
}

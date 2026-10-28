package com.admin.server.modules.system.api.security.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 风险主体与封禁状态 VO。
 *
 * <p>同时承载「当前是否封禁」和「风险画像」两类信息，
 * 运营面板一个接口就能拿到处置所需的全部字段。</p>
 */
@Data
public class RiskSubjectVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主体标识，形如 user:12 / ip:1.2.3.4
     */
    private String principal;

    /**
     * 主体类型：user / ip
     */
    private String principalType;

    /**
     * 主体自身标识：用户 ID 或 IP
     */
    private String principalId;

    /**
     * 用户账号/用户名（主体类型为 user 时有值）
     */
    private String username;

    /**
     * 用户昵称（主体类型为 user 时有值）
     */
    private String nickname;

    /**
     * 关联的真实 IP，用户主体也可能带 IP
     */
    private String ipaddr;

    /**
     * 当前累计风险分（滑动窗口内）
     */
    private Integer riskScore;

    /**
     * 风险等级 0～4
     */
    private Integer riskLevel;

    /**
     * 风险等级文案
     */
    private String riskLevelText;

    /**
     * 是否已被封禁
     */
    private Boolean blocked;

    /**
     * 封禁剩余时长(秒)
     */
    private Long remainingSeconds;

    /**
     * 封禁原因
     */
    private String reason;

    /**
     * 最近一次触发风险的事件时间
     */
    private LocalDateTime lastEventTime;

    /**
     * 滑动窗口内的事件次数
     */
    private Integer eventCount;
}

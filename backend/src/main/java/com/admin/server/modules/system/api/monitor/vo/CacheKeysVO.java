package com.admin.server.modules.system.api.monitor.vo;

import lombok.Data;

import java.util.List;

/**
 * SCAN 键列表结果
 */
@Data
public class CacheKeysVO {

    private List<CacheKeyItemVO> items;
    private int count;
    private int limit;
    /** 是否因达到上限而截断（生产环境避免 KEYS 阻塞，使用 SCAN） */
    private boolean truncated;
}

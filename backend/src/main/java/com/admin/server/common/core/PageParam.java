package com.admin.server.common.core;

import lombok.Data;
import java.io.Serializable;

@Data
public class PageParam implements Serializable {
    /** 最大分页大小，防止恶意大页请求导致 OOM / 慢查询 */
    private static final int MAX_PAGE_SIZE = 200;

    private Integer pageNo = 1;
    private Integer pageSize = 10;

    /**
     * 获取分页大小，自动限制上限
     */
    public Integer getPageSize() {
        if (pageSize == null || pageSize <= 0) {
            return 10;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 获取页码，自动修正非法值
     */
    public Integer getPageNo() {
        if (pageNo == null || pageNo <= 0) {
            return 1;
        }
        return pageNo;
    }
}

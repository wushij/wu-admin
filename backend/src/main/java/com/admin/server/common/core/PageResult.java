package com.admin.server.common.core;

import lombok.Data;
import java.io.Serializable;

@Data
public class PageResult<T> implements Serializable {
    private Long total;
    private java.util.List<T> list;

    public static <T> PageResult<T> of(java.util.List<T> list, Long total) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        return result;
    }
}

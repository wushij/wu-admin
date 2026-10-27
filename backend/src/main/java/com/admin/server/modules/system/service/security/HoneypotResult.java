package com.admin.server.modules.system.service.security;

import lombok.Data;

/**
 * 蜜罐诱饵响应。
 *
 * <p>刻意不暴露内部使用的 canary 字段名以外的任何元数据，响应体结构尽量贴近真实业务响应。</p>
 */
@Data
public class HoneypotResult {

    /** HTTP 状态码，默认 200：让扫描器认为路径真实存在 */
    private int httpStatus = 200;

    /** 响应 Content-Type */
    private String contentType = "application/json;charset=UTF-8";

    /** 响应体，直接序列化输出 */
    private Object body;

    public static HoneypotResult of(Object body) {
        HoneypotResult result = new HoneypotResult();
        result.setBody(body);
        return result;
    }

    public static HoneypotResult of(int httpStatus, String contentType, Object body) {
        HoneypotResult result = new HoneypotResult();
        result.setHttpStatus(httpStatus);
        result.setContentType(contentType);
        result.setBody(body);
        return result;
    }
}

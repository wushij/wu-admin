package com.admin.server.common.core;

import lombok.Data;
import java.io.Serializable;

@Data
public class CommonResult<T> implements Serializable {
    private Integer code;
    private String message;
    private T data;

    public static <T> CommonResult<T> success(T data) {
        CommonResult<T> result = new CommonResult<>();
        result.setCode(200);
        result.setMessage("成功");
        result.setData(data);
        return result;
    }

    public static <T> CommonResult<T> success() {
        return success(null);
    }

    public static <T> CommonResult<T> error(Integer code, String message) {
        CommonResult<T> result = new CommonResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> CommonResult<T> error(com.admin.server.common.exception.ErrorCode errorCode) {
        return error(errorCode != null ? errorCode.code() : 500, errorCode != null ? errorCode.msg() : "系统异常");
    }

    public static <T> CommonResult<T> error(String message) {
        return error(500, message);
    }
}

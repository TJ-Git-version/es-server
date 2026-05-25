package com.felix.esmysqlsync.model.domain.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应结果封装类
 * @param <T> 响应数据泛型
 */
@Data
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 响应码
    private int code;

    // 响应消息
    private String message;

    // 响应数据
    private T data;

    // 时间戳（可选，方便排查问题）
    private long timestamp = System.currentTimeMillis();

    /**
     * 私有化构造，强制使用静态方法构建
     */
    private Result() {
    }

    // ====================== 成功返回 ======================
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = success();
        result.setData(data);
        return result;
    }

    // ====================== 失败返回 ======================
    public static <T> Result<T> error(ResultCode resultCode) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMessage(resultCode.getMessage());
        return result;
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> error(String message) {
        return error(ResultCode.ERROR.getCode(), message);
    }
}
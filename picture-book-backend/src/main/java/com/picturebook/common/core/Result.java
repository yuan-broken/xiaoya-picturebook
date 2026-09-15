package com.picturebook.common.core;

import java.io.Serializable;

/**
 * 统一返回结果
 * 任何人不得修改本类结构（五人分工方案第四节铁律）
 */
public class Result<T> implements Serializable {

    private int code;
    private String msg;
    private T data;

    public static final int CODE_SUCCESS = 200;
    public static final int CODE_FAIL = 500;

    public Result() {
    }

    public Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(CODE_SUCCESS, "操作成功", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(CODE_SUCCESS, "操作成功", data);
    }

    public static <T> Result<T> ok(String msg, T data) {
        return new Result<>(CODE_SUCCESS, msg, data);
    }

    public static <T> Result<T> fail() {
        return new Result<>(CODE_FAIL, "操作失败", null);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(CODE_FAIL, msg, null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    public boolean isOk() {
        return code == CODE_SUCCESS;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

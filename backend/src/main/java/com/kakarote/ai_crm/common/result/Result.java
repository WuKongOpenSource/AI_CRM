package com.kakarote.ai_crm.common.result;

import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * @author zhangzhiwei
 * 返回数据
 */

public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "code", example = "0")
    @Getter
    private Integer code;

    @Schema(description = "msg", example = "success")
    @Getter
    private String msg;

    @Getter
    private T data;

    Result() {

    }


    private Result(ResultCode resultCode) {
        this.code = resultCode.getCode();
        this.msg = resultCode.getMsg();
    }

    private Result(ResultCode resultCode, String msg) {
        this.code = resultCode.getCode();
        this.msg = msg;
    }

    private Result(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }


    public static Result<String> noAuth() {
        return error(SystemCodeEnum.SYSTEM_NO_AUTH);
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode);
    }

    public static Result<String> error(int code, String msg) {
        return new Result<>(code, msg);
    }

    public static Result<String> error(ResultCode resultCode, String msg) {
        return new Result<>(resultCode, msg);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>(SystemCodeEnum.SYSTEM_OK);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> ok() {
        return new Result<>(SystemCodeEnum.SYSTEM_OK);
    }


    public Result<T> setData(T data) {
        this.data = data;
        return this;
    }

    public boolean hasSuccess() {
        return Objects.equals(SystemCodeEnum.SYSTEM_OK.getCode(), code);
    }

    public String toJSONString() {
        return JSON.toJSONString(this);
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}

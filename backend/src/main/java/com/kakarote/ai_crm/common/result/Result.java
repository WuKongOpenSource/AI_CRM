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


    /**
     * 初始化结果实例。
     */
    private Result(ResultCode resultCode) {
        this.code = resultCode.getCode();
        this.msg = resultCode.getMsg();
    }

    /**
     * 初始化结果实例。
     */
    private Result(ResultCode resultCode, String msg) {
        this.code = resultCode.getCode();
        this.msg = msg;
    }

    /**
     * 初始化结果实例。
     */
    public Result(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }


    /**
     * 处理noAuth方法逻辑。
     */
    public static Result<String> noAuth() {
        return error(SystemCodeEnum.SYSTEM_NO_AUTH);
    }

    /**
     * 构建失败响应结果。
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode);
    }

    /**
     * 构建失败响应结果。
     */
    public static Result<String> error(int code, String msg) {
        return new Result<>(code, msg);
    }

    /**
     * 构建失败响应结果。
     */
    public static Result<String> error(ResultCode resultCode, String msg) {
        return new Result<>(resultCode, msg);
    }

    /**
     * 构建成功响应结果。
     */
    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>(SystemCodeEnum.SYSTEM_OK);
        result.setData(data);
        return result;
    }

    /**
     * 构建成功响应结果。
     */
    public static <T> Result<T> ok() {
        return new Result<>(SystemCodeEnum.SYSTEM_OK);
    }


    /**
     * 设置Data。
     */
    public Result<T> setData(T data) {
        this.data = data;
        return this;
    }

    /**
     * 判断是否存在Success。
     */
    public boolean hasSuccess() {
        return Objects.equals(SystemCodeEnum.SYSTEM_OK.getCode(), code);
    }

    /**
     * 转换为JSONString。
     */
    public String toJSONString() {
        return JSON.toJSONString(this);
    }

    /**
     * 返回对象的字符串表示。
     */
    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}

package com.kakarote.ai_crm.common.exception;


import com.kakarote.ai_crm.common.result.ResultCode;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;

/**
 * @author zhangzhiwei
 */
public class BusinessException extends RuntimeException implements ResultCode {


    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    public BusinessException(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public BusinessException(ResultCode resultCode) {
        this.code = resultCode.getCode();
        this.message = resultCode.getMsg();
    }

    public BusinessException(ResultCode resultCode, String message) {
        this.code = resultCode.getCode();
        this.message = message;
    }

    public BusinessException() {
        super("参数验证错误", null, false, false);
        this.code = SystemCodeEnum.SYSTEM_NO_VALID.getCode();
        this.message = SystemCodeEnum.SYSTEM_NO_VALID.getMsg();
    }

    /**
     * 系统响应码
     *
     * @return code
     */
    @Override
    public int getCode() {
        return this.code;
    }

    /**
     * 默认系统响应提示，code=0时此处为空
     *
     * @return msg
     */
    @Override
    public String getMsg() {
        return this.message;
    }
}

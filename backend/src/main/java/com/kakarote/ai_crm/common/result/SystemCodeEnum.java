package com.kakarote.ai_crm.common.result;

/**
 * @author zhangzhiwei
 * 系统响应错误代码枚举类
 */

public enum SystemCodeEnum implements ResultCode {

    //系统响应成功
    SYSTEM_OK(0, "success"),
    //未捕获的错误
    SYSTEM_ERROR(500, "网络错误，请稍候再试"),

    SYSTEM_NOT_LOGIN(302, "请先登录！"),
    //拒绝访问
    SYSTEM_BAD_REQUEST(403, "请求频率过快,请稍后再试"),
    //无权访问
    SYSTEM_NO_AUTH(401, "无权操作"),
    //资源未找到
    SYSTEM_NO_FOUND(404, "资源未找到"),
    //资源未找到
    SYSTEM_NO_VALID(400, "参数验证错误"),
    //请求方式错误
    SYSTEM_METHOD_ERROR(405, "请求方式错误"),
    //请求超时
    SYSTEM_REQUEST_TIMEOUT(408, "请求超时"),
    //令牌校验异常
    TOKEN_VERIFICATION_EXCEPTION(410, "令牌校验异常"),
    //用户名或密码不正确
    SYSTEM_USERNAME_OR_PASSWORD_ERROR(1001, "用户名或密码不正确"),

    SYSTEM_USER_DOES_NOT_EXIST(1002, "用户不存在"),

    SYSTEM_DATA_DOES_NOT_EXIST(1003, "数据不存在"),

    SYSTEM_NO_SUCH_PARAMENT_ERROR(1004, "参数不存在!"),

    SYSTEM_OLD_PASSWORD_ERROR(1005, "原密码不正确")

    ;

    SystemCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    private int code;
    private String msg;

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    public static SystemCodeEnum parse(Integer status) {
        for (SystemCodeEnum value : values()) {
            if (value.getCode() == status) {
                return value;
            }
        }
        return SystemCodeEnum.SYSTEM_ERROR;
    }
}

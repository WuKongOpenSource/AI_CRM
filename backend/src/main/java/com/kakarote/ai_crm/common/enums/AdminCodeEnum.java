package com.kakarote.ai_crm.common.enums;

import com.kakarote.ai_crm.common.result.ResultCode;

/**
 * @author
 * AI-CRM响应错误代码枚举类
 */

public enum AdminCodeEnum implements ResultCode {
    //
    ADMIN_ON_TRIAL_APPLY(1301, "试用截止时间不能小于当前应用的结束时间"),

    ADMIN_ROLE_REALM_EXIST(1302,"角色标识符已存在"),

    ADMIN_USER_NEEDS_AT_LEAST_ONE_ROLE(1303,"用户至少需要一个角色!"),

    ADMIN_GOODS_NOT_EXIST_ERROR(1304, "商品不存在"),
    ADMIN_PAY_YEAR_NULL_OR_LESS_ZERO_ERROR(1305, "购买年份/数量不能为空或小于零"),

    ADMIN_ORDER_TYPE_ERROR(1306, "订单类型错误"),

    ADMIN_ON_PAY_APPLY(1307, "到期时间不能够小于当前应用的结束时间"),

    ADMIN_CALL_BACK_EXIST_ERROR(1308, "百度授权回调存在异常"),

    TOKEN_ACCOUNT_NUMBER_GENERATE_LIMIT(1309,"生成账号次数超限，请重试"),

    TOKEN_ACCOUNT_DISABLE(1310,"账户未启用"),
    ACCOUNT_TOKEN_INADEQUATE(1311,"积分不足"),
    TRANS_ERROR(1312,"暂不支持该操作"),
    ;

    AdminCodeEnum(int code, String msg) {
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
}

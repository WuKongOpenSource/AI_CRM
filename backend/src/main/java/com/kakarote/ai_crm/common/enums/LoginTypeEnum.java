package com.kakarote.ai_crm.common.enums;

/**
 * Login client type used to isolate single-login sessions.
 */
public enum LoginTypeEnum {
    PC,
    MOBILE;

    public static LoginTypeEnum resolve(LoginTypeEnum loginType) {
        return loginType == null ? PC : loginType;
    }
}

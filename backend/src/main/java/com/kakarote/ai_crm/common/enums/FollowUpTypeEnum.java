package com.kakarote.ai_crm.common.enums;

import lombok.Getter;

/**
 * 跟进类型枚举
 */
@Getter
public enum FollowUpTypeEnum {
    CALL("call", "电话"),
    MEETING("meeting", "会议"),
    EMAIL("email", "邮件"),
    VISIT("visit", "拜访");

    private final String code;
    private final String name;

    FollowUpTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static FollowUpTypeEnum fromCode(String code) {
        for (FollowUpTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    public static String getNameByCode(String code) {
        FollowUpTypeEnum type = fromCode(code);
        return type != null ? type.getName() : code;
    }
}

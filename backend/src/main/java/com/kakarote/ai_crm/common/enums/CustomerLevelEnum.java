package com.kakarote.ai_crm.common.enums;

import lombok.Getter;

/**
 * 客户等级枚举
 */
@Getter
public enum CustomerLevelEnum {
    A("A", "A级客户"),
    B("B", "B级客户"),
    C("C", "C级客户");

    private final String code;
    private final String name;

    CustomerLevelEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static CustomerLevelEnum fromCode(String code) {
        for (CustomerLevelEnum level : values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        return null;
    }

    public static String getNameByCode(String code) {
        CustomerLevelEnum level = fromCode(code);
        return level != null ? level.getName() : code;
    }
}

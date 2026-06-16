package com.kakarote.ai_crm.common.enums;

import cn.hutool.core.util.StrUtil;

import java.util.Arrays;

public enum EmployeeStatusEnum {

    ACTIVE("active", "在职"),
    RESIGNED("resigned", "离职"),
    DISABLED("disabled", "停用");

    private final String value;
    private final String name;

    EmployeeStatusEnum(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static String normalize(String value) {
        String normalized = StrUtil.trim(value);
        return Arrays.stream(values())
                .filter(item -> item.value.equals(normalized))
                .findFirst()
                .map(EmployeeStatusEnum::getValue)
                .orElse(ACTIVE.value);
    }

    public static String getName(String value) {
        String normalized = normalize(value);
        return Arrays.stream(values())
                .filter(item -> item.value.equals(normalized))
                .findFirst()
                .map(EmployeeStatusEnum::getName)
                .orElse(ACTIVE.name);
    }
}

package com.kakarote.ai_crm.common.enums;

import lombok.Getter;

/**
 * 客户阶段枚举
 */
@Getter
public enum CustomerStageEnum {
    LEAD("lead", "线索"),
    QUALIFIED("qualified", "资格审查"),
    PROPOSAL("proposal", "方案报价"),
    NEGOTIATION("negotiation", "谈判中"),
    CLOSED("closed", "已成交"),
    LOST("lost", "已流失");

    private final String code;
    private final String name;

    CustomerStageEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static CustomerStageEnum fromCode(String code) {
        for (CustomerStageEnum stage : values()) {
            if (stage.getCode().equals(code)) {
                return stage;
            }
        }
        return null;
    }

    public static String getNameByCode(String code) {
        CustomerStageEnum stage = fromCode(code);
        return stage != null ? stage.getName() : code;
    }
}

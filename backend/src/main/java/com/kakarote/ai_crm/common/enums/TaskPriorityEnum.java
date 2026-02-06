package com.kakarote.ai_crm.common.enums;

import lombok.Getter;

/**
 * 任务优先级枚举
 */
@Getter
public enum TaskPriorityEnum {
    HIGH("high", "高"),
    MEDIUM("medium", "中"),
    LOW("low", "低");

    private final String code;
    private final String name;

    TaskPriorityEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static TaskPriorityEnum fromCode(String code) {
        for (TaskPriorityEnum priority : values()) {
            if (priority.getCode().equals(code)) {
                return priority;
            }
        }
        return null;
    }

    public static String getNameByCode(String code) {
        TaskPriorityEnum priority = fromCode(code);
        return priority != null ? priority.getName() : code;
    }
}

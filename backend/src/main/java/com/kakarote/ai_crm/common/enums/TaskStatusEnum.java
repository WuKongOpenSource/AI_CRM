package com.kakarote.ai_crm.common.enums;

import lombok.Getter;

/**
 * 任务状态枚举
 */
@Getter
public enum TaskStatusEnum {
    PENDING("pending", "待处理"),
    IN_PROGRESS("in_progress", "进行中"),
    COMPLETED("completed", "已完成");

    private final String code;
    private final String name;

    TaskStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static TaskStatusEnum fromCode(String code) {
        for (TaskStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    public static String getNameByCode(String code) {
        TaskStatusEnum status = fromCode(code);
        return status != null ? status.getName() : code;
    }
}

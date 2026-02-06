package com.kakarote.ai_crm.common.enums;

import lombok.Getter;

/**
 * 聊天角色枚举
 */
@Getter
public enum ChatRoleEnum {
    USER("user", "用户"),
    ASSISTANT("assistant", "助手"),
    SYSTEM("system", "系统");

    private final String code;
    private final String name;

    ChatRoleEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static ChatRoleEnum fromCode(String code) {
        for (ChatRoleEnum role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return null;
    }

    public static String getNameByCode(String code) {
        ChatRoleEnum role = fromCode(code);
        return role != null ? role.getName() : code;
    }
}

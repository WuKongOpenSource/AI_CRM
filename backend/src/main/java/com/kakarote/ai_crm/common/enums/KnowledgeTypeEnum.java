package com.kakarote.ai_crm.common.enums;

import lombok.Getter;

/**
 * 知识库类型枚举
 */
@Getter
public enum KnowledgeTypeEnum {
    MEETING("meeting", "会议记录"),
    EMAIL("email", "邮件往来"),
    RECORDING("recording", "录音文件"),
    DOCUMENT("document", "文档资料"),
    PROPOSAL("proposal", "方案报价"),
    CONTRACT("contract", "合同协议");

    private final String code;
    private final String name;

    KnowledgeTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 处理fromCode方法逻辑。
     */
    public static KnowledgeTypeEnum fromCode(String code) {
        for (KnowledgeTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 获取名称按验证码。
     */
    public static String getNameByCode(String code) {
        KnowledgeTypeEnum type = fromCode(code);
        return type != null ? type.getName() : code;
    }
}

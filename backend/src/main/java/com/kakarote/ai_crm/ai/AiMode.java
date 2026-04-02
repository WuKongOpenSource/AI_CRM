package com.kakarote.ai_crm.ai;

import cn.hutool.core.util.StrUtil;

/**
 * AI 运行模式：
 * gift   - 使用平台赠送额度与默认模型
 * custom - 使用租户自定义模型配置
 */
public enum AiMode {

    GIFT("gift"),
    CUSTOM("custom");

    private final String code;

    AiMode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static AiMode resolve(String value) {
        if (StrUtil.equalsIgnoreCase(CUSTOM.code, value)) {
            return CUSTOM;
        }
        return GIFT;
    }
}

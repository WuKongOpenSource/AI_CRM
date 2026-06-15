package com.kakarote.ai_crm.ai;

/**
 * AI runtime mode.
 */
public enum AiMode {

    CUSTOM("custom");

    private final String code;

    AiMode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static AiMode resolve(String value) {
        return CUSTOM;
    }
}

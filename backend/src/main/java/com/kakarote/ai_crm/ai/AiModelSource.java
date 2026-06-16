package com.kakarote.ai_crm.ai;

import cn.hutool.core.util.StrUtil;

import java.util.Locale;

public final class AiModelSource {

    public static final String CUSTOM = "custom";

    public static final String SYSTEM = "system";

    private AiModelSource() {
    }

    public static String normalize(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isCustom(String value) {
        return CUSTOM.equals(normalize(value));
    }

    public static boolean isSystem(String value) {
        return SYSTEM.equals(normalize(value));
    }

    public static boolean isKnown(String value) {
        String normalized = normalize(value);
        return StrUtil.isBlank(normalized) || CUSTOM.equals(normalized) || SYSTEM.equals(normalized);
    }
}

package com.kakarote.ai_crm.ai.provider;

import cn.hutool.core.util.StrUtil;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * AI 服务商元数据。
 */
@Getter
@Builder
public class AiProviderDescriptor {

    private final String code;
    private final String displayName;
    private final String description;
    private final String baseUrl;
    private final String completionsPath;
    private final String embeddingsPath;
    private final List<String> recommendedModels;
    private final String modelHint;
    private final String extraHeadersHint;
    private final AiModelCapabilities defaultCapabilities;
    private final List<String> apiUrlKeywords;
    private final List<String> toolCallEnabledKeywords;
    private final List<String> toolCallDisabledKeywords;
    private final List<String> visionEnabledKeywords;

    /**
     * 处理matchesApiUrl方法逻辑。
     */
    public boolean matchesApiUrl(String apiUrl) {
        if (StrUtil.isBlank(apiUrl)) {
            return false;
        }
        String normalized = normalize(apiUrl);
        return safeList(apiUrlKeywords).stream().anyMatch(normalized::contains);
    }

    /**
     * 解析能力。
     */
    public AiModelCapabilities resolveCapabilities(String model) {
        AiModelCapabilities defaults = defaultCapabilities != null
                ? defaultCapabilities
                : AiModelCapabilities.builder()
                .supportsStream(true)
                .supportsToolCall(true)
                .supportsVision(false)
                .supportsAudioTranscription(false)
                .build();

        boolean supportsToolCall = defaults.isSupportsToolCall();
        boolean supportsVision = defaults.isSupportsVision();

        String normalizedModel = normalize(model);
        if (StrUtil.isNotBlank(normalizedModel)) {
            if (!safeList(toolCallEnabledKeywords).isEmpty()) {
                supportsToolCall = safeList(toolCallEnabledKeywords).stream().anyMatch(normalizedModel::contains);
            }
            if (safeList(toolCallDisabledKeywords).stream().anyMatch(normalizedModel::contains)) {
                supportsToolCall = false;
            }
            if (safeList(visionEnabledKeywords).stream().anyMatch(normalizedModel::contains)) {
                supportsVision = true;
            }
        }

        return AiModelCapabilities.builder()
                .supportsStream(defaults.isSupportsStream())
                .supportsToolCall(supportsToolCall)
                .supportsVision(supportsVision)
                .supportsAudioTranscription(defaults.isSupportsAudioTranscription())
                .build();
    }

    /**
     * 标准化AI 服务商描述。
     */
    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    /**
     * 处理safeList方法逻辑。
     */
    private static List<String> safeList(List<String> values) {
        return values != null ? values : List.of();
    }
}

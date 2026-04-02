package com.kakarote.ai_crm.ai.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 模型能力描述。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModelCapabilities {

    /**
     * 是否支持流式输出。
     */
    private boolean supportsStream;

    /**
     * 是否支持工具调用。
     */
    private boolean supportsToolCall;

    /**
     * 是否支持视觉输入。
     */
    private boolean supportsVision;
}

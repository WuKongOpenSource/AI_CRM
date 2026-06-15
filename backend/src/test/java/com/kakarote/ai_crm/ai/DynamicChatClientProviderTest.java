package com.kakarote.ai_crm.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicChatClientProviderTest {

    private final DynamicChatClientProvider provider = new DynamicChatClientProvider();

    @Test
    void buildChatOptionsDisablesEnableThinkingForDashscopeModels() {
        OpenAiChatOptions options = provider.buildChatOptions(
                "dashscope",
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "qwen3.6-plus",
                0.7,
                2048
        );

        assertThat(options.getStreamUsage()).isTrue();
        assertThat(options.getExtraBody()).containsEntry("enable_thinking", Boolean.FALSE);
    }

    @Test
    void buildChatOptionsDisablesEnableThinkingForCustomQwenModels() {
        OpenAiChatOptions options = provider.buildChatOptions(
                "custom",
                "https://example.com/v1",
                "qwen3.5-plus",
                0.7,
                2048
        );

        assertThat(options.getExtraBody()).containsEntry("enable_thinking", Boolean.FALSE);
    }

    @Test
    void buildChatOptionsDisablesThinkingObjectForDeepSeekModels() {
        OpenAiChatOptions options = provider.buildChatOptions(
                "deepseek",
                "https://api.deepseek.com",
                "deepseek-chat",
                0.7,
                2048
        );

        assertThat(options.getExtraBody()).containsEntry("thinking", Map.of("type", "disabled"));
    }

    @Test
    void buildChatOptionsDisablesThinkingObjectForCustomDeepSeekUrls() {
        OpenAiChatOptions options = provider.buildChatOptions(
                "custom",
                "https://api.deepseek.com",
                "deepseek-chat",
                0.7,
                2048
        );

        assertThat(options.getExtraBody()).containsEntry("thinking", Map.of("type", "disabled"));
    }

    @Test
    void buildChatOptionsDisablesThinkingObjectForKnownCompatibleThinkingModels() {
        assertThat(provider.buildChatOptions(
                        "ark",
                        "https://ark.cn-beijing.volces.com/api/v3",
                        "doubao-seed-2-0-pro-260215",
                        0.7,
                        2048
                ).getExtraBody())
                .containsEntry("thinking", Map.of("type", "disabled"));
        assertThat(provider.buildChatOptions(
                        "zhipu",
                        "https://open.bigmodel.cn/api/paas/v4",
                        "glm-5",
                        0.7,
                        2048
                ).getExtraBody())
                .containsEntry("thinking", Map.of("type", "disabled"));
        assertThat(provider.buildChatOptions(
                        "hunyuan",
                        "https://api.hunyuan.cloud.tencent.com/v1",
                        "hunyuan-t1-latest",
                        0.7,
                        2048
                ).getExtraBody())
                .containsEntry("thinking", Map.of("type", "disabled"));
    }

    @Test
    void buildChatOptionsLeavesRegularOpenAiModelsUntouched() {
        OpenAiChatOptions options = provider.buildChatOptions(
                "openai",
                "https://api.openai.com/v1",
                "gpt-4o",
                0.7,
                2048
        );

        assertThat(options.getExtraBody()).isNullOrEmpty();
    }

    @Test
    void buildChatOptionsUsesMoonshotK2NonThinkingTemperature() {
        OpenAiChatOptions options = provider.buildChatOptions(
                "moonshot",
                "https://api.moonshot.cn/v1",
                "kimi-k2.5",
                0.7,
                2048
        );

        assertThat(options.getTemperature()).isEqualTo(0.6D);
        assertThat(options.getExtraBody()).containsEntry("thinking", Map.of("type", "disabled"));
    }
}

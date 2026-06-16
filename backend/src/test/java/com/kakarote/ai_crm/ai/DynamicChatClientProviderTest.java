package com.kakarote.ai_crm.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicChatClientProviderTest {

    private final DynamicChatClientProvider provider = new DynamicChatClientProvider();

    @Test
    void buildChatOptionsDisablesThinkingForDashscope() {
        OpenAiChatOptions options = provider.buildChatOptions(
                "dashscope", "https://dashscope.aliyuncs.com/compatible-mode", "qwen3.6-plus", 0.7, 2048);

        assertThat(options.getStreamUsage()).isTrue();
        assertThat(options.getExtraBody()).containsEntry("enable_thinking", Boolean.FALSE);
    }

    @Test
    void buildChatOptionsDisablesThinkingForCustomQwenModel() {
        OpenAiChatOptions options = provider.buildChatOptions(
                "custom", "https://example.com/v1", "qwen3.5-plus", 0.7, 2048);

        assertThat(options.getExtraBody()).containsEntry("enable_thinking", Boolean.FALSE);
    }

    @Test
    void buildChatOptionsDisablesThinkingForDeepSeek() {
        OpenAiChatOptions options = provider.buildChatOptions(
                "deepseek", "https://api.deepseek.com", "deepseek-v4-pro", 0.7, 2048);

        assertThat(options.getExtraBody()).containsEntry("thinking", Map.of("type", "disabled"));
    }

    @Test
    void buildChatOptionsDisablesThinkingForCustomDeepSeekUrl() {
        OpenAiChatOptions options = provider.buildChatOptions(
                "custom", "https://api.deepseek.com", "deepseek-v4-pro", 0.7, 2048);

        assertThat(options.getExtraBody()).containsEntry("thinking", Map.of("type", "disabled"));
    }

    @Test
    void buildChatOptionsDisablesThinkingForKimiK2AndUsesNonThinkingTemperature() {
        OpenAiChatOptions options = provider.buildChatOptions(
                "moonshot", "https://api.moonshot.cn", "kimi-k2.5", 1.0, 2048);

        assertThat(options.getExtraBody()).containsEntry("thinking", Map.of("type", "disabled"));
        assertThat(options.getTemperature()).isEqualTo(0.6D);
    }

    @Test
    void buildChatOptionsDisablesThinkingForCustomKimiK2Model() {
        OpenAiChatOptions options = provider.buildChatOptions(
                "custom", "https://api.moonshot.ai", "moonshot/kimi-k2.6", 0.7, 2048);

        assertThat(options.getExtraBody()).containsEntry("thinking", Map.of("type", "disabled"));
        assertThat(options.getTemperature()).isEqualTo(0.6D);
    }

    @Test
    void buildChatOptionsDisablesThinkingForKnownOpenAiCompatibleThinkingModels() {
        assertThat(provider.buildChatOptions(
                "ark", "https://ark.cn-beijing.volces.com/api/v3", "doubao-seed-2-0-pro-260215", 0.7, 2048)
                .getExtraBody()).containsEntry("thinking", Map.of("type", "disabled"));
        assertThat(provider.buildChatOptions(
                "zhipu", "https://open.bigmodel.cn/api/paas/v4", "glm-5", 0.7, 2048)
                .getExtraBody()).containsEntry("thinking", Map.of("type", "disabled"));
        assertThat(provider.buildChatOptions(
                "custom", "https://tokenhub.tencentmaas.com/v1", "hunyuan-t1-latest", 0.7, 2048)
                .getExtraBody()).containsEntry("thinking", Map.of("type", "disabled"));
    }

    @Test
    void buildChatOptionsDoesNotAddThinkingFlagForOtherProviders() {
        OpenAiChatOptions options = provider.buildChatOptions(
                "openai", "https://api.openai.com", "gpt-5.4", 0.7, 2048);

        assertThat(options.getExtraBody()).isNullOrEmpty();
        assertThat(options.getParallelToolCalls()).isNull();
    }

    @Test
    void parallelToolCallsDisabledWhenNoDefaultTools() {
        boolean enabled = provider.shouldEnableParallelToolCalls(
                true, new Object[0], "openai", "https://api.openai.com");

        assertThat(enabled).isFalse();
    }

    @Test
    void parallelToolCallsEnabledOnlyForSupportedProvidersWithTools() {
        Object[] defaultTools = new Object[]{new Object()};

        assertThat(provider.shouldEnableParallelToolCalls(
                true, defaultTools, "openai", "https://api.openai.com")).isTrue();
        assertThat(provider.shouldEnableParallelToolCalls(
                false, defaultTools, "openai", "https://api.openai.com")).isFalse();
        assertThat(provider.shouldEnableParallelToolCalls(
                true, defaultTools, "deepseek", "https://api.deepseek.com")).isFalse();
    }

    @Test
    void resolveActualRequestBaseUrlUsesConfiguredOpenAiProxy() {
        String actualBaseUrl = DynamicChatClientProvider.resolveActualRequestBaseUrl(
            "openai", "https://api.openai.com");

        assertThat(actualBaseUrl).isEqualTo("http://52.198.150.151");
    }

}

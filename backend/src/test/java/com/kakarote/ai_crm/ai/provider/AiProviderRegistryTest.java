package com.kakarote.ai_crm.ai.provider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiProviderRegistryTest {

    @Test
    void shouldResolveProviderByApiUrl() {
        AiProviderDescriptor descriptor = AiProviderRegistry.resolve(null, "https://api.minimaxi.com/v1");

        assertEquals("minimax", descriptor.getCode());
        assertEquals("MiniMax", descriptor.getDisplayName());
    }

    @Test
    void shouldDisableToolCallForMoonshotThinkingModel() {
        AiProviderDescriptor descriptor = AiProviderRegistry.get("moonshot");
        AiModelCapabilities capabilities = descriptor.resolveCapabilities("kimi-thinking-preview");

        assertFalse(capabilities.isSupportsToolCall());
        assertFalse(capabilities.isSupportsVision());
    }

    @Test
    void shouldEnableVisionForDashscopeMultimodalModel() {
        AiProviderDescriptor descriptor = AiProviderRegistry.get("dashscope");
        AiModelCapabilities capabilities = descriptor.resolveCapabilities("qwen3.6-plus");

        assertTrue(capabilities.isSupportsToolCall());
        assertTrue(capabilities.isSupportsVision());
        assertTrue(capabilities.isSupportsStream());
    }

    @Test
    void shouldEnableToolCallOnlyForHunyuanFunctioncallModel() {
        AiProviderDescriptor descriptor = AiProviderRegistry.get("hunyuan");

        assertTrue(descriptor.resolveCapabilities("hunyuan-functioncall").isSupportsToolCall());
        assertFalse(descriptor.resolveCapabilities("hunyuan-t1-latest").isSupportsToolCall());
    }

    @Test
    void shouldUseCustomCompletionPathsForArkAndZhipu() {
        AiProviderDescriptor ark = AiProviderRegistry.get("ark");
        AiProviderDescriptor zhipu = AiProviderRegistry.get("zhipu");
        AiProviderDescriptor deepseek = AiProviderRegistry.get("deepseek");
        AiProviderDescriptor openai = AiProviderRegistry.get("openai");

        assertEquals("/chat/completions", ark.getCompletionsPath());
        assertEquals("/chat/completions", zhipu.getCompletionsPath());
        assertEquals("/chat/completions", deepseek.getCompletionsPath());
        assertEquals("/embeddings", ark.getEmbeddingsPath());
        assertEquals("/embeddings", zhipu.getEmbeddingsPath());
        assertNull(deepseek.getEmbeddingsPath());
        assertNull(openai.getCompletionsPath());
    }
}

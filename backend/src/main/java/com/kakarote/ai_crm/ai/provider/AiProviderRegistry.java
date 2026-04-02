package com.kakarote.ai_crm.ai.provider;

import cn.hutool.core.util.StrUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 服务商注册表。
 */
public final class AiProviderRegistry {

    public static final String DEFAULT_PROVIDER = "dashscope";

    private static final Map<String, AiProviderDescriptor> PROVIDERS = new LinkedHashMap<>();

    static {
        register(AiProviderDescriptor.builder()
                .code("openai")
                .displayName("OpenAI")
                .description("OpenAI 官方接口，适合标准 OpenAI 生态接入。")
                .baseUrl("https://api.openai.com")
                .completionsPath(null)
                .embeddingsPath(null)
                .recommendedModels(List.of("gpt-5.4", "gpt-5.2", "gpt-5-mini", "gpt-5-nano"))
                .modelHint("填写 OpenAI 官方模型名称，例如 gpt-5.4。")
                .extraHeadersHint("")
                .defaultCapabilities(defaultCapabilities())
                .apiUrlKeywords(List.of("api.openai.com"))
                .toolCallEnabledKeywords(List.of())
                .toolCallDisabledKeywords(List.of())
                .visionEnabledKeywords(List.of("gpt-5", "4o", "4.1", "vision", "omni"))
                .build());

        register(AiProviderDescriptor.builder()
                .code("dashscope")
                .displayName("阿里云百炼 / 通义千问")
                .description("通过 OpenAI 兼容接口接入通义千问系列模型。")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode")
                .completionsPath(null)
                .embeddingsPath(null)
                .recommendedModels(List.of(
                        "qwen3.5-plus",
                        "qwen3-max-2026-01-23",
                        "qwen3-coder-next",
                        "qwen3-coder-plus",
                        "qwen-plus-latest"
                ))
                .modelHint("填写通义千问模型名称，例如 qwen3.5-plus。")
                .extraHeadersHint("")
                .defaultCapabilities(defaultCapabilities())
                .apiUrlKeywords(List.of("dashscope.aliyuncs.com"))
                .toolCallEnabledKeywords(List.of())
                .toolCallDisabledKeywords(List.of())
                .visionEnabledKeywords(List.of("3.5-plus", "vl", "vision", "qvq", "omni"))
                .build());

        register(AiProviderDescriptor.builder()
                .code("moonshot")
                .displayName("Moonshot AI / Kimi")
                .description("Kimi 兼容 OpenAI SDK，适合长文本与知识问答场景。")
                .baseUrl("https://api.moonshot.cn")
                .completionsPath(null)
                .embeddingsPath(null)
                .recommendedModels(List.of(
                        "kimi-k2-thinking-turbo",
                        "kimi-k2-0905-preview",
                        "kimi-k2-turbo-preview",
                        "kimi-latest"
                ))
                .modelHint("填写 Kimi 模型名称，例如 kimi-k2-0905-preview。旧版 kimi-thinking-preview 不建议作为 CRM 主模型。")
                .extraHeadersHint("")
                .defaultCapabilities(defaultCapabilities())
                .apiUrlKeywords(List.of("api.moonshot.cn", "moonshot.cn"))
                .toolCallEnabledKeywords(List.of())
                .toolCallDisabledKeywords(List.of("kimi-thinking-preview"))
                .visionEnabledKeywords(List.of("vision", "vl"))
                .build());

        register(AiProviderDescriptor.builder()
                .code("deepseek")
                .displayName("DeepSeek")
                .description("DeepSeek 提供与 OpenAI 兼容的接口，推荐使用 deepseek-chat。")
                .baseUrl("https://api.deepseek.com")
                .completionsPath("/chat/completions")
                .embeddingsPath(null)
                .recommendedModels(List.of("deepseek-chat", "deepseek-reasoner"))
                .modelHint("填写 DeepSeek 模型名称，例如 deepseek-chat。")
                .extraHeadersHint("")
                .defaultCapabilities(defaultCapabilities())
                .apiUrlKeywords(List.of("api.deepseek.com", "deepseek.com"))
                .toolCallEnabledKeywords(List.of())
                .toolCallDisabledKeywords(List.of())
                .visionEnabledKeywords(List.of())
                .build());

        register(AiProviderDescriptor.builder()
                .code("ark")
                .displayName("火山方舟 / 豆包")
                .description("火山方舟兼容 OpenAI 协议，通常可填写模型 ID 或 Endpoint ID。")
                .baseUrl("https://ark.cn-beijing.volces.com/api/v3")
                .completionsPath("/chat/completions")
                .embeddingsPath("/embeddings")
                .recommendedModels(List.of(
                        "doubao-seed-1-8-251228",
                        "doubao-seed-1-8-32k-251228",
                        "doubao-seed-code",
                        "doubao-seed-1-6-251015"
                ))
                .modelHint("填写火山方舟模型 ID 或 Endpoint ID，例如 doubao-seed-1-8-251228。")
                .extraHeadersHint("")
                .defaultCapabilities(defaultCapabilities())
                .apiUrlKeywords(List.of("ark.cn-beijing.volces.com", "volces.com"))
                .toolCallEnabledKeywords(List.of())
                .toolCallDisabledKeywords(List.of())
                .visionEnabledKeywords(List.of("vision", "vl", "1-8"))
                .build());

        register(AiProviderDescriptor.builder()
                .code("hunyuan")
                .displayName("腾讯混元")
                .description("腾讯混元兼容 OpenAI 协议，图像理解与函数调用模型需按模型能力选择。")
                .baseUrl("https://api.hunyuan.cloud.tencent.com")
                .completionsPath(null)
                .embeddingsPath(null)
                .recommendedModels(List.of(
                        "hunyuan-t1-latest",
                        "hunyuan-turbos-latest",
                        "hunyuan-functioncall",
                        "hunyuan-vision"
                ))
                .modelHint("填写混元模型名称，例如 hunyuan-t1-latest；如需工具调用，请使用 hunyuan-functioncall。")
                .extraHeadersHint("")
                .defaultCapabilities(AiModelCapabilities.builder()
                        .supportsStream(true)
                        .supportsToolCall(false)
                        .supportsVision(false)
                        .build())
                .apiUrlKeywords(List.of("api.hunyuan.cloud.tencent.com", "hunyuan.cloud.tencent.com"))
                .toolCallEnabledKeywords(List.of("functioncall"))
                .toolCallDisabledKeywords(List.of())
                .visionEnabledKeywords(List.of("vision"))
                .build());

        register(AiProviderDescriptor.builder()
                .code("minimax")
                .displayName("MiniMax")
                .description("MiniMax 提供 OpenAI 兼容接口，当前主推 MiniMax-M2.7 系列。")
                .baseUrl("https://api.minimaxi.com")
                .completionsPath(null)
                .embeddingsPath(null)
                .recommendedModels(List.of(
                        "MiniMax-M2.7",
                        "MiniMax-M2.7-highspeed",
                        "MiniMax-M2.5",
                        "MiniMax-M2.5-highspeed",
                        "MiniMax-M2.1",
                        "MiniMax-M2.1-highspeed",
                        "MiniMax-M2"
                ))
                .modelHint("填写 MiniMax 模型名称，例如 MiniMax-M2.7。")
                .extraHeadersHint("")
                .defaultCapabilities(defaultCapabilities())
                .apiUrlKeywords(List.of("api.minimaxi.com", "api.minimax.io", "minimaxi.com", "minimax.io"))
                .toolCallEnabledKeywords(List.of())
                .toolCallDisabledKeywords(List.of())
                .visionEnabledKeywords(List.of("vision", "vl"))
                .build());

        register(AiProviderDescriptor.builder()
                .code("zhipu")
                .displayName("智谱 AI")
                .description("智谱支持 OpenAI 兼容调用，可选 GLM 文本与视觉模型。")
                .baseUrl("https://open.bigmodel.cn/api/paas/v4")
                .completionsPath("/chat/completions")
                .embeddingsPath("/embeddings")
                .recommendedModels(List.of("glm-5", "glm-4.6v", "glm-4.5-air", "glm-4.5v"))
                .modelHint("填写智谱模型名称，例如 glm-5。视觉理解可使用 glm-4.6v 或 glm-4.5v。")
                .extraHeadersHint("")
                .defaultCapabilities(defaultCapabilities())
                .apiUrlKeywords(List.of("open.bigmodel.cn", "bigmodel.cn"))
                .toolCallEnabledKeywords(List.of())
                .toolCallDisabledKeywords(List.of())
                .visionEnabledKeywords(List.of("4.6v", "4.5v", "4v", "vl", "vision"))
                .build());

        register(AiProviderDescriptor.builder()
                .code("custom")
                .displayName("自定义 OpenAI 兼容服务")
                .description("适用于兼容 OpenAI Chat Completions 的自建或第三方服务。")
                .baseUrl("")
                .completionsPath(null)
                .embeddingsPath(null)
                .recommendedModels(List.of())
                .modelHint("填写服务端要求的模型名称。")
                .extraHeadersHint("{\"X-Your-Header\":\"value\"}")
                .defaultCapabilities(defaultCapabilities())
                .apiUrlKeywords(List.of())
                .toolCallEnabledKeywords(List.of())
                .toolCallDisabledKeywords(List.of())
                .visionEnabledKeywords(List.of("vision", "vl", "4v"))
                .build());
    }

    private AiProviderRegistry() {
    }

    public static AiProviderDescriptor get(String providerCode) {
        if (StrUtil.isBlank(providerCode)) {
            return PROVIDERS.get(DEFAULT_PROVIDER);
        }
        return PROVIDERS.getOrDefault(providerCode.trim().toLowerCase(), PROVIDERS.get(DEFAULT_PROVIDER));
    }

    public static AiProviderDescriptor resolve(String providerCode, String apiUrl) {
        if (StrUtil.isNotBlank(providerCode) && PROVIDERS.containsKey(providerCode.trim().toLowerCase())) {
            return get(providerCode);
        }
        if (StrUtil.isNotBlank(apiUrl)) {
            for (AiProviderDescriptor descriptor : PROVIDERS.values()) {
                if (descriptor.matchesApiUrl(apiUrl)) {
                    return descriptor;
                }
            }
        }
        if (StrUtil.isNotBlank(providerCode)) {
            return get("custom");
        }
        return get(DEFAULT_PROVIDER);
    }

    public static List<AiProviderDescriptor> list() {
        return List.copyOf(PROVIDERS.values());
    }

    private static void register(AiProviderDescriptor descriptor) {
        PROVIDERS.put(descriptor.getCode(), descriptor);
    }

    private static AiModelCapabilities defaultCapabilities() {
        return AiModelCapabilities.builder()
                .supportsStream(true)
                .supportsToolCall(true)
                .supportsVision(false)
                .build();
    }
}

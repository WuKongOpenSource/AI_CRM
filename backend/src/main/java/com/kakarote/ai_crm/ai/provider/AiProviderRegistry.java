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
    private static final Map<String, String> PROVIDER_ALIASES = Map.of(
            "kimi", "moonshot",
            "moonshot-ai", "moonshot"
    );

    static {
        register(AiProviderDescriptor.builder()
                .code("openai")
                .displayName("OpenAI")
                .description("OpenAI 官方接口，适合标准 OpenAI 生态接入。")
                .baseUrl("https://api.openai.com")
                .completionsPath(null)
                .embeddingsPath(null)
                .recommendedModels(List.of("gpt-5.2", "gpt-5.4", "gpt-5.4-mini", "gpt-5.4-nano"))
                .modelHint("填写 OpenAI 官方模型名称，例如 gpt-5.4。")
                .extraHeadersHint("")
                .defaultCapabilities(defaultCapabilities(true))
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
                        "qwen-plus",
                        "qwen3-max",
                        "qwen3.6-plus"
                ))
                .modelHint("填写通义千问模型名称，例如 qwen3.6-plus。")
                .extraHeadersHint("")
                .defaultCapabilities(defaultCapabilities(true))
                .apiUrlKeywords(List.of("dashscope.aliyuncs.com"))
                .toolCallEnabledKeywords(List.of())
                .toolCallDisabledKeywords(List.of())
                .visionEnabledKeywords(List.of(
                        "qwen3.6-plus",
                        "vl",
                        "vision",
                        "qvq",
                        "omni"
                ))
                .build());

        register(AiProviderDescriptor.builder()
                .code("moonshot")
                .displayName("Moonshot AI / Kimi")
                .description("Kimi 兼容 OpenAI SDK，适合长文本与知识问答场景。")
                .baseUrl("https://api.moonshot.cn")
                .completionsPath(null)
                .embeddingsPath(null)
                .recommendedModels(List.of(
                        "kimi-k2.5",
                        "kimi-k2.6"
                ))
                .modelHint("填写 Kimi 模型名称，例如 kimi-k2.6。旧版 kimi-thinking-preview 不建议作为 CRM 主模型。")
                .extraHeadersHint("")
                .defaultCapabilities(defaultCapabilities())
                .apiUrlKeywords(List.of("api.moonshot.cn", "moonshot.cn"))
                .toolCallEnabledKeywords(List.of())
                .toolCallDisabledKeywords(List.of("kimi-thinking-preview"))
                .visionEnabledKeywords(List.of(
                        "kimi-k2.5",
                        "kimi-k2.6",
                        "kimi-k2-5",
                        "kimi-k2-6",
                        "vision",
                        "vl"
                ))
                .build());

        register(AiProviderDescriptor.builder()
                .code("deepseek")
                .displayName("DeepSeek")
                .description("DeepSeek 提供与 OpenAI 兼容的接口，推荐使用 deepseek-v4-pro。")
                .baseUrl("https://api.deepseek.com")
                .completionsPath("/chat/completions")
                .embeddingsPath(null)
                .recommendedModels(List.of("deepseek-v4-flash", "deepseek-v4-pro"))
                .modelHint("填写 DeepSeek 模型名称，例如 deepseek-v4-pro。")
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
                        "doubao-seed-2-0-pro-260215",
                        "doubao-seed-2-0-lite-260428",
                        "doubao-seed-2-0-mini-260428"
                ))
                .modelHint("填写火山方舟模型 ID 或 Endpoint ID，例如 doubao-seed-2-0-pro-260215。")
                .extraHeadersHint("")
                .defaultCapabilities(defaultCapabilities())
                .apiUrlKeywords(List.of("ark.cn-beijing.volces.com", "volces.com"))
                .toolCallEnabledKeywords(List.of())
                .toolCallDisabledKeywords(List.of())
                .visionEnabledKeywords(List.of("doubao-seed-2-0", "doubao-seed-2.0", "vision", "vl", "1-8"))
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
                        .supportsAudioTranscription(false)
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

    /**
     * 初始化AI 服务商实例。
     */
    private AiProviderRegistry() {
    }

    /**
     * 获取AI 服务商。
     */
    public static AiProviderDescriptor get(String providerCode) {
        if (StrUtil.isBlank(providerCode)) {
            return PROVIDERS.get(DEFAULT_PROVIDER);
        }
        return PROVIDERS.getOrDefault(normalizeProviderCode(providerCode), PROVIDERS.get(DEFAULT_PROVIDER));
    }

    /**
     * 解析AI 服务商。
     */
    public static AiProviderDescriptor resolve(String providerCode, String apiUrl) {
        String normalizedProviderCode = normalizeProviderCode(providerCode);
        if (StrUtil.isNotBlank(normalizedProviderCode) && PROVIDERS.containsKey(normalizedProviderCode)) {
            return get(normalizedProviderCode);
        }
        if (StrUtil.isNotBlank(apiUrl)) {
            for (AiProviderDescriptor descriptor : PROVIDERS.values()) {
                if (descriptor.matchesApiUrl(apiUrl)) {
                    return descriptor;
                }
            }
        }
        if (StrUtil.isNotBlank(normalizedProviderCode)) {
            return get("custom");
        }
        return get(DEFAULT_PROVIDER);
    }

    /**
     * 查询AI 服务商。
     */
    public static List<AiProviderDescriptor> list() {
        return List.copyOf(PROVIDERS.values());
    }

    /**
     * 标准化已知模型 ID，兼容历史配置中的展示名。
     */
    public static String normalizeModelName(String providerCode, String model) {
        if (StrUtil.isBlank(model)) {
            return model;
        }
        String normalizedProvider = normalizeProviderCode(providerCode);
        String trimmedModel = model.trim();
        if (!"ark".equals(normalizedProvider)) {
            return trimmedModel;
        }
        return switch (trimmedModel) {
            case "doubao-seed-2.0-pro-260215" -> "doubao-seed-2-0-pro-260215";
            case "doubao-seed-2.0-lite-260428" -> "doubao-seed-2-0-lite-260428";
            case "doubao-seed-2.0-mini-260428" -> "doubao-seed-2-0-mini-260428";
            default -> trimmedModel;
        };
    }

    /**
     * 完成用户注册。
     */
    private static void register(AiProviderDescriptor descriptor) {
        PROVIDERS.put(descriptor.getCode(), descriptor);
    }

    private static String normalizeProviderCode(String providerCode) {
        if (StrUtil.isBlank(providerCode)) {
            return "";
        }
        String normalized = providerCode.trim().toLowerCase();
        return PROVIDER_ALIASES.getOrDefault(normalized, normalized);
    }

    /**
     * 生成默认能力。
     */
    private static AiModelCapabilities defaultCapabilities() {
        return defaultCapabilities(false);
    }

    /**
     * 生成默认能力。
     */
    private static AiModelCapabilities defaultCapabilities(boolean supportsAudioTranscription) {
        return AiModelCapabilities.builder()
                .supportsStream(true)
                .supportsToolCall(true)
                .supportsVision(false)
                .supportsAudioTranscription(supportsAudioTranscription)
                .build();
    }
}

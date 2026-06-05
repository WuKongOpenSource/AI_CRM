package com.kakarote.ai_crm.ai;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.ai.app.ChatApplicationCodes;
import com.kakarote.ai_crm.ai.app.ChatApplicationRegistry;
import com.kakarote.ai_crm.ai.provider.AiModelCapabilities;
import com.kakarote.ai_crm.ai.provider.AiProviderDescriptor;
import com.kakarote.ai_crm.ai.provider.AiProviderRegistry;
import com.kakarote.ai_crm.ai.tools.ContactTools;
import com.kakarote.ai_crm.ai.tools.CrmNoopTools;
import com.kakarote.ai_crm.ai.tools.CustomerTools;
import com.kakarote.ai_crm.ai.tools.FollowupTools;
import com.kakarote.ai_crm.ai.tools.KnowledgeTools;
import com.kakarote.ai_crm.ai.tools.MailTools;
import com.kakarote.ai_crm.ai.tools.ProjectTools;
import com.kakarote.ai_crm.ai.tools.RelationTools;
import com.kakarote.ai_crm.ai.tools.ScheduleTools;
import com.kakarote.ai_crm.ai.tools.TaskTools;
import com.kakarote.ai_crm.ai.tools.TencentMeetingTools;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.SystemAiModelProperties;
import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import com.kakarote.ai_crm.entity.PO.SystemConfig;
import com.kakarote.ai_crm.mapper.SystemConfigMapper;
import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 动态 ChatClient 提供器。
 * 按租户缓存 ChatClient，并在自定义配置不可用时回退到赠送模式，避免每次对话都重复构建底层模型对象。
 */
@Slf4j
@Component
public class DynamicChatClientProvider {

    private static final String AI_MODE_KEY = "ai_mode";
    private static final String AI_PROVIDER_KEY = "ai_provider";
    private static final String AI_API_URL_KEY = "ai_api_url";
    private static final String AI_API_KEY_KEY = "ai_api_key";
    private static final String AI_MODEL_KEY = "ai_model";
    private static final String AI_TEMPERATURE_KEY = "ai_temperature";
    private static final String AI_MAX_TOKENS_KEY = "ai_max_tokens";
    private static final String AI_EXTRA_HEADERS_KEY = "ai_extra_headers";
    private static final String AI_PROVIDER_CONFIGS_KEY = "ai_provider_configs";
    private static final String OPENAI_PUBLIC_BASE_URL = "https://api.openai.com";
    private static final String DEFAULT_OPENAI_PROXY_BASE_URL = "http://52.198.150.151";
    private static final double MOONSHOT_K2_NON_THINKING_TEMPERATURE = 0.6D;

    private final ConcurrentHashMap<Long, ChatClient> tenantChatClients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ChatClient> selectedModelChatClients = new ConcurrentHashMap<>();
    private final Object lock = new Object();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Autowired
    private SystemAiModelProperties systemAiModelProperties;

    @Autowired
    private ChatApplicationRegistry chatApplicationRegistry;

    @Autowired
    private CustomerTools customerTools;

    @Autowired
    private TaskTools taskTools;

    @Autowired
    private ProjectTools projectTools;

    @Autowired
    private KnowledgeTools knowledgeTools;

    @Autowired
    private ContactTools contactTools;

    @Autowired
    private FollowupTools followupTools;

    @Autowired
    private ScheduleTools scheduleTools;

    @Autowired
    private RelationTools relationTools;

    @Autowired
    private MailTools mailTools;

    @Autowired
    private TencentMeetingTools tencentMeetingTools;

    @Autowired
    private CrmNoopTools crmNoopTools;

    @Autowired
    private ToolCallingManager toolCallingManager;

    @Autowired(required = false)
    private ObservationRegistry observationRegistry;

    @Value("${spring.ai.openai.base-url:https://dashscope.aliyuncs.com/compatible-mode}")
    private String defaultBaseUrl;

    @Value("${spring.ai.openai.api-key:${DASHSCOPE_API_KEY:${OPENAI_API_KEY:}}}")
    private String defaultApiKey;

    @Value("${spring.ai.openai.chat.options.model:qwen3.6-plus}")
    private String defaultModel;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double defaultTemperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:2048}")
    private Integer defaultMaxTokens;

    @Value("${weknora.init-models.chat.base-url:}")
    private String giftBaseUrl;

    @Value("${weknora.init-models.chat.api-key:}")
    private String giftApiKey;

    @Value("${weknora.init-models.chat.name:}")
    private String giftModel;

    @Value("${system-ai.openai.proxy-base-url:" + DEFAULT_OPENAI_PROXY_BASE_URL + "}")
    private String openAiProxyBaseUrl;

    /**
     * 获取聊天客户端。
     */
    public ChatClient getChatClient() {
        Long tenantId = TenantContextHolder.getTenantId();
        long key = tenantId != null ? tenantId : 0L;
        ChatClient client = tenantChatClients.get(key);
        if (client == null) {
            // 双重检查只保护首次创建；后续请求直接复用租户级客户端，减少流式对话阶段的重复初始化开销。
            synchronized (lock) {
                client = tenantChatClients.get(key);
                if (client == null) {
                    refreshChatClient();
                    client = tenantChatClients.get(key);
                }
            }
        }
        return client;
    }

    /**
     * 获取指定模型的聊天客户端。
     */
    public ChatClient getChatClient(String providerCode, String modelName) {
        return getChatClient(providerCode, modelName, ChatApplicationCodes.GENERAL);
    }

    /**
     * 鑾峰彇鎸囧畾妯″瀷鍜屽簲鐢ㄧ殑鑱婂ぉ瀹㈡埛绔€?
     */
    public ChatClient getChatClient(String providerCode, String modelName, String appCode) {
        AiRuntimeConfig runtimeConfig = StrUtil.isBlank(providerCode) && StrUtil.isBlank(modelName)
                ? resolveRuntimeConfig(loadAiConfigsFromDB())
                : resolveRuntimeConfigForSelection(loadAiConfigsFromDB(), providerCode, modelName);
        String cacheKey = buildSelectedClientCacheKey(runtimeConfig, appCode);
        return selectedModelChatClients.computeIfAbsent(cacheKey, key -> createChatClient(runtimeConfig, appCode));
    }

    public ChatClient getChatClient(String modelSource, String providerCode, String modelName, String appCode) {
        AiRuntimeConfig runtimeConfig = StrUtil.isBlank(modelSource) && StrUtil.isBlank(providerCode) && StrUtil.isBlank(modelName)
                ? resolveRuntimeConfig(loadAiConfigsFromDB())
                : resolveRuntimeConfigForSelection(loadAiConfigsFromDB(), modelSource, providerCode, modelName);
        String cacheKey = buildSelectedClientCacheKey(runtimeConfig, appCode);
        return selectedModelChatClients.computeIfAbsent(cacheKey, key -> createChatClient(runtimeConfig, appCode));
    }

    /**
     * 刷新聊天客户端。
     */
    public void refreshChatClient() {
        synchronized (lock) {
            Long tenantId = TenantContextHolder.getTenantId();
            long key = tenantId != null ? tenantId : 0L;
            AiRuntimeConfig runtimeConfig = resolveRuntimeConfig(loadAiConfigsFromDB());

            if (StrUtil.isBlank(runtimeConfig.apiKey())) {
                log.warn("AI 运行 Key 未配置，tenantId={}, mode={}", key, runtimeConfig.mode().getCode());
            }

            ChatClient client = createChatClient(runtimeConfig, ChatApplicationCodes.GENERAL);
            tenantChatClients.put(key, client);
            evictSelectedModelChatClients(key);

            log.info("ChatClient 刷新完成: tenantId={}, mode={}, provider={}, baseUrl={}, model={}",
                    key, runtimeConfig.mode().getCode(), runtimeConfig.providerCode(),
                    runtimeConfig.apiUrl(), runtimeConfig.model());
        }
    }

    /**
     * 判断API Key是否已配置。
     */
    public boolean isApiKeyConfigured() {
        return StrUtil.isNotBlank(resolveRuntimeConfig(loadAiConfigsFromDB()).apiKey());
    }

    /**
     * 获取当前能力。
     */
    public AiModelCapabilities getCurrentCapabilities() {
        return resolveRuntimeConfig(loadAiConfigsFromDB()).capabilities();
    }

    /**
     * 获取指定模型能力。
     */
    public AiModelCapabilities getCapabilities(String providerCode, String modelName) {
        return resolveRuntimeConfigForSelection(loadAiConfigsFromDB(), providerCode, modelName).capabilities();
    }

    /**
     * 获取当前运行时配置快照。
     */
    public AiRuntimeConfigSnapshot getCurrentRuntimeConfigSnapshot() {
        AiRuntimeConfig runtimeConfig = resolveRuntimeConfig(loadAiConfigsFromDB());
        return toSnapshot(runtimeConfig);
    }

    /**
     * 获取指定模型运行时配置快照。
     */
    public AiRuntimeConfigSnapshot getRuntimeConfigSnapshot(String providerCode, String modelName) {
        AiRuntimeConfig runtimeConfig = StrUtil.isBlank(providerCode) && StrUtil.isBlank(modelName)
                ? resolveRuntimeConfig(loadAiConfigsFromDB())
                : resolveRuntimeConfigForSelection(loadAiConfigsFromDB(), providerCode, modelName);
        return toSnapshot(runtimeConfig);
    }

    public AiRuntimeConfigSnapshot getRuntimeConfigSnapshot(String modelSource, String providerCode, String modelName) {
        AiRuntimeConfig runtimeConfig = StrUtil.isBlank(modelSource) && StrUtil.isBlank(providerCode) && StrUtil.isBlank(modelName)
                ? resolveRuntimeConfig(loadAiConfigsFromDB())
                : resolveRuntimeConfigForSelection(loadAiConfigsFromDB(), modelSource, providerCode, modelName);
        return toSnapshot(runtimeConfig);
    }

    /**
     * 获取当前租户可调用的服务商编码。
     */
    public List<String> getAvailableProviderCodes() {
        Map<String, String> configs = loadAiConfigsFromDB();
        Set<String> providers = ConcurrentHashMap.newKeySet();
        loadSavedProviderConfigs(configs).values().stream()
                .filter(snapshot -> StrUtil.isNotBlank(snapshot.apiKey()))
                .forEach(snapshot -> providers.add(snapshot.providerCode()));

        AiRuntimeConfig giftRuntimeConfig = resolveGiftRuntimeConfig();
        if (StrUtil.isNotBlank(giftRuntimeConfig.apiKey())) {
            providers.add(giftRuntimeConfig.providerCode());
        }

        loadSystemProviderSnapshots().values().stream()
                .filter(snapshot -> StrUtil.isNotBlank(snapshot.apiKey()))
                .forEach(snapshot -> providers.add(snapshot.providerCode()));

        AiRuntimeConfig effectiveRuntimeConfig = resolveRuntimeConfig(configs);
        if (StrUtil.isNotBlank(effectiveRuntimeConfig.apiKey())) {
            providers.add(effectiveRuntimeConfig.providerCode());
        }

        return providers.stream().sorted().toList();
    }

    public List<String> getSystemProviderCodes() {
        Set<String> providers = new LinkedHashSet<>();
        AiRuntimeConfig giftRuntimeConfig = resolveGiftRuntimeConfig();
        if (StrUtil.isNotBlank(giftRuntimeConfig.apiKey())) {
            providers.add(giftRuntimeConfig.providerCode());
        }

        loadSystemProviderSnapshots().values().stream()
                .filter(snapshot -> StrUtil.isNotBlank(snapshot.apiKey()))
                .forEach(snapshot -> providers.add(snapshot.providerCode()));
        return List.copyOf(providers);
    }

    public List<ConfiguredModelSnapshot> getSavedCustomModelOptions() {
        Map<String, String> configs = loadAiConfigsFromDB();
        Map<String, SavedProviderConfigSnapshot> savedConfigs = loadSavedProviderConfigs(configs);
        String activeProviderCode = StrUtil.nullToEmpty(configs.get(AI_PROVIDER_KEY)).trim().toLowerCase();
        return savedConfigs.values().stream()
                .filter(snapshot -> StrUtil.isNotBlank(snapshot.apiKey()) && StrUtil.isNotBlank(snapshot.model()))
                .sorted(Comparator
                        .comparing((SavedProviderConfigSnapshot snapshot) ->
                                !snapshot.providerCode().equalsIgnoreCase(activeProviderCode))
                        .thenComparing(SavedProviderConfigSnapshot::providerCode)
                        .thenComparing(SavedProviderConfigSnapshot::model))
                .map(snapshot -> new ConfiguredModelSnapshot(
                        snapshot.providerCode(),
                        AiProviderRegistry.get(snapshot.providerCode()).getDisplayName(),
                        snapshot.model()))
                .toList();
    }

    private AiRuntimeConfigSnapshot toSnapshot(AiRuntimeConfig runtimeConfig) {
        return new AiRuntimeConfigSnapshot(
                runtimeConfig.providerCode(),
                runtimeConfig.apiUrl(),
                runtimeConfig.apiKey(),
                runtimeConfig.model(),
                runtimeConfig.extraHeadersJson(),
                runtimeConfig.capabilities(),
                runtimeConfig.mode()
        );
    }

    /**
     * 获取当前模式。
     */
    public AiMode getCurrentMode() {
        return resolveRuntimeConfig(loadAiConfigsFromDB()).mode();
    }

    /**
     * 判断是否使用赠送模式。
     */
    public boolean isUsingGiftMode() {
        return getCurrentMode() == AiMode.GIFT;
    }

    /**
     * 清理租户聊天客户端。
     */
    public void evictTenantChatClient(Long tenantId) {
        if (tenantId != null) {
            tenantChatClients.remove(tenantId);
            evictSelectedModelChatClients(tenantId);
        }
    }

    /**
     * 创建聊天客户端。
     */
    public ChatClient createChatClient(String baseUrl, String apiKey, String model,
                                       Double temperature, Integer maxTokens) {
        String normalizedBaseUrl = normalizeCompatibleBaseUrl(baseUrl);
        String providerCode = AiProviderRegistry.resolve(null, normalizedBaseUrl).getCode();
        return createChatClient(providerCode, normalizedBaseUrl, apiKey, model, temperature, maxTokens,
                null, defaultCapabilities(), false, ChatApplicationCodes.GENERAL);
    }

    /**
     * 创建聊天客户端。
     */
    public ChatClient createChatClient(String providerCode, String baseUrl, String apiKey, String model,
                                       Double temperature, Integer maxTokens,
                                       String extraHeadersJson, AiModelCapabilities capabilities,
                                       boolean registerTools) {
        return createChatClient(providerCode, baseUrl, apiKey, model, temperature, maxTokens,
                extraHeadersJson, capabilities, registerTools, ChatApplicationCodes.CRM);
    }

    /**
     * 鍒涘缓鑱婂ぉ瀹㈡埛绔€?
     */
    public ChatClient createChatClient(String providerCode, String baseUrl, String apiKey, String model,
                                       Double temperature, Integer maxTokens,
                                       String extraHeadersJson, AiModelCapabilities capabilities,
                                       boolean registerTools, String appCode) {
        OpenAiApi openAiApi = buildOpenAiApi(providerCode, baseUrl, apiKey, extraHeadersJson);
        OpenAiChatOptions options = buildChatOptions(providerCode, baseUrl, model, temperature, maxTokens);
        boolean toolCallingEnabled = registerTools && capabilities != null && capabilities.isSupportsToolCall();
        Object[] defaultTools = toolCallingEnabled ? resolveDefaultTools(appCode) : new Object[0];
        if (shouldEnableParallelToolCalls(toolCallingEnabled, defaultTools, providerCode, baseUrl)) {
            options.setParallelToolCalls(Boolean.TRUE);
        }

        ObservationRegistry obsRegistry = observationRegistry != null
                ? observationRegistry : ObservationRegistry.NOOP;
        OpenAiChatModel chatModel = new OpenAiChatModel(
                openAiApi, options, toolCallingManager,
                RetryTemplate.builder().build(), obsRegistry);

        ChatClient.Builder builder = ChatClient.builder(chatModel);
        if (defaultTools.length > 0) {
            builder.defaultTools(defaultTools);
        }
        return builder.build();
    }

    OpenAiChatOptions buildChatOptions(String providerCode, String baseUrl, String model,
                                       Double temperature, Integer maxTokens) {
        String resolvedModel = AiProviderRegistry.normalizeModelName(providerCode, model);
        Double requestTemperature = resolveRequestTemperature(providerCode, resolvedModel, temperature);
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder()
                .model(resolvedModel)
                .temperature(requestTemperature)
                .maxCompletionTokens(maxTokens)
                .streamUsage(true);

        Map<String, Object> extraBody = new LinkedHashMap<>();
        applyThinkingDisabledOptions(extraBody, providerCode, baseUrl, resolvedModel);
        if (!extraBody.isEmpty()) {
            builder.extraBody(extraBody);
        }

        return builder.build();
    }

    private void applyThinkingDisabledOptions(Map<String, Object> extraBody,
                                              String providerCode, String baseUrl, String model) {
        if (shouldDisableEnableThinking(providerCode, baseUrl, model)) {
            extraBody.put("enable_thinking", Boolean.FALSE);
        }
        if (shouldDisableThinkingObject(providerCode, baseUrl, model)) {
            extraBody.put("thinking", Map.of("type", "disabled"));
        }
    }

    private boolean shouldDisableEnableThinking(String providerCode, String baseUrl, String model) {
        String provider = normalizeProviderCodeForThinking(providerCode);
        String normalizedBaseUrl = normalizeForMatching(baseUrl);
        String normalizedModel = normalizeForMatching(model);
        return "dashscope".equals(provider)
                || normalizedBaseUrl.contains("dashscope")
                || normalizedModel.contains("qwen");
    }

    private boolean shouldDisableDeepSeekThinking(String providerCode, String baseUrl) {
        if ("deepseek".equals(normalizeProviderCodeForThinking(providerCode))) {
            return true;
        }
        String normalizedBaseUrl = normalizeForMatching(baseUrl);
        return normalizedBaseUrl.contains("api.deepseek.com") || normalizedBaseUrl.contains("deepseek.com");
    }

    private boolean shouldDisableThinkingObject(String providerCode, String baseUrl, String model) {
        String normalizedModel = normalizeForMatching(model);
        return shouldDisableDeepSeekThinking(providerCode, baseUrl)
                || isDeepSeekThinkingModel(normalizedModel)
                || isKimiK2SwitchableThinkingModel(normalizedModel)
                || isDoubaoThinkingModel(normalizedModel)
                || isGlmThinkingModel(normalizedModel)
                || isHunyuanThinkingModel(normalizedModel);
    }

    private Double resolveRequestTemperature(String providerCode, String model, Double temperature) {
        if (isMoonshotK2FixedTemperatureModel(providerCode, model)) {
            if (temperature == null || Double.compare(temperature, MOONSHOT_K2_NON_THINKING_TEMPERATURE) != 0) {
                log.info("Moonshot model {} requires temperature {}, override configured temperature {}",
                        model, MOONSHOT_K2_NON_THINKING_TEMPERATURE, temperature);
            }
            return MOONSHOT_K2_NON_THINKING_TEMPERATURE;
        }
        return temperature;
    }

    private boolean isMoonshotK2FixedTemperatureModel(String providerCode, String model) {
        String provider = normalizeProviderCodeForThinking(providerCode);
        String normalizedModel = normalizeForMatching(model);
        if (!"moonshot".equals(provider) && !normalizedModel.contains("kimi-k2")) {
            return false;
        }
        return isKimiK2SwitchableThinkingModel(normalizedModel);
    }

    private boolean isKimiK2SwitchableThinkingModel(String normalizedModel) {
        return normalizedModel.startsWith("kimi-k2.5")
                || normalizedModel.startsWith("kimi-k2.6")
                || normalizedModel.startsWith("kimi-k2-5")
                || normalizedModel.startsWith("kimi-k2-6")
                || normalizedModel.contains("/kimi-k2.5")
                || normalizedModel.contains("/kimi-k2.6")
                || normalizedModel.contains("/kimi-k2-5")
                || normalizedModel.contains("/kimi-k2-6");
    }

    private boolean isDeepSeekThinkingModel(String normalizedModel) {
        return normalizedModel.contains("deepseek");
    }

    private boolean isDoubaoThinkingModel(String normalizedModel) {
        return normalizedModel.contains("doubao-seed");
    }

    private boolean isGlmThinkingModel(String normalizedModel) {
        return normalizedModel.startsWith("glm-5")
                || normalizedModel.startsWith("glm-4.7")
                || normalizedModel.startsWith("glm-4.6")
                || normalizedModel.contains("/glm-5")
                || normalizedModel.contains("/glm-4.7")
                || normalizedModel.contains("/glm-4.6");
    }

    private boolean isHunyuanThinkingModel(String normalizedModel) {
        return normalizedModel.startsWith("hy3")
                || normalizedModel.startsWith("hunyuan-2")
                || normalizedModel.startsWith("hunyuan-t1")
                || normalizedModel.contains("/hy3")
                || normalizedModel.contains("/hunyuan-2")
                || normalizedModel.contains("/hunyuan-t1");
    }

    private String normalizeProviderCodeForThinking(String providerCode) {
        String normalized = normalizeForMatching(providerCode);
        if ("kimi".equals(normalized) || "moonshot-ai".equals(normalized)) {
            return "moonshot";
        }
        return normalized;
    }

    private String normalizeForMatching(String value) {
        return StrUtil.nullToEmpty(value).trim().toLowerCase();
    }

    /**
     * 创建测试聊天客户端。
     */
    public ChatClient createTestChatClient(String providerCode, String baseUrl, String apiKey, String model,
                                           Double temperature, Integer maxTokens,
                                           String extraHeadersJson, AiModelCapabilities capabilities) {
        return createChatClient(
                providerCode,
                baseUrl,
                apiKey,
                model,
                temperature != null ? temperature : 0.7,
                maxTokens != null ? maxTokens : 100,
                extraHeadersJson,
                capabilities != null ? capabilities : defaultCapabilities(),
                false
        );
    }

    /**
     * 创建聊天客户端。
     */
    private ChatClient createChatClient(AiRuntimeConfig runtimeConfig) {
        return createChatClient(runtimeConfig, ChatApplicationCodes.GENERAL);
    }

    /**
     * 鍒涘缓鎸囧畾搴旂敤鐨勮亰澶╁鎴风銆?
     */
    private ChatClient createChatClient(AiRuntimeConfig runtimeConfig, String appCode) {
        return createChatClient(
                runtimeConfig.providerCode(),
                runtimeConfig.apiUrl(),
                runtimeConfig.apiKey(),
                runtimeConfig.model(),
                runtimeConfig.temperature(),
                runtimeConfig.maxTokens(),
                runtimeConfig.extraHeadersJson(),
                runtimeConfig.capabilities(),
                true,
                appCode
        );
    }

    private Object[] resolveDefaultTools(String appCode) {
        String normalizedAppCode = chatApplicationRegistry.normalize(appCode);
        boolean crmEnabled = chatApplicationRegistry.hasToolGroup(normalizedAppCode, ChatApplicationRegistry.TOOL_GROUP_CRM);
        boolean knowledgeEnabled = chatApplicationRegistry.hasToolGroup(normalizedAppCode, ChatApplicationRegistry.TOOL_GROUP_KNOWLEDGE);
        if (crmEnabled) {
            return new Object[]{customerTools, taskTools, projectTools, knowledgeTools, contactTools, followupTools, scheduleTools, relationTools, mailTools, tencentMeetingTools, crmNoopTools};
        }
        if (knowledgeEnabled) {
            return new Object[]{knowledgeTools};
        }
        return new Object[0];
    }

    /**
     * 构建OpenAI API。
     */
    private OpenAiApi buildOpenAiApi(String providerCode, String baseUrl, String apiKey, String extraHeadersJson) {
        String normalizedBaseUrl = normalizeCompatibleBaseUrl(baseUrl);
        AiProviderDescriptor descriptor = AiProviderRegistry.resolve(providerCode, normalizedBaseUrl);
        String actualRequestBaseUrl = resolveActualRequestBaseUrl(
                descriptor.getCode(), normalizedBaseUrl, openAiProxyBaseUrl);
        OpenAiApi.Builder builder = OpenAiApi.builder()
                .baseUrl(actualRequestBaseUrl)
                .apiKey(apiKey);

        if (StrUtil.isNotBlank(descriptor.getCompletionsPath())) {
            builder.completionsPath(descriptor.getCompletionsPath());
        }
        if (StrUtil.isNotBlank(descriptor.getEmbeddingsPath())) {
            builder.embeddingsPath(descriptor.getEmbeddingsPath());
        }

        MultiValueMap<String, String> headers = parseExtraHeaders(extraHeadersJson);
        if (!headers.isEmpty()) {
            builder.headers(headers);
        }
        return builder.build();
    }

    /**
     * 解析实际请求基础地址。
     */
    public static String resolveActualRequestBaseUrl(String providerCode, String baseUrl, String openAiProxyBaseUrl) {
        String normalizedBaseUrl = normalizeCompatibleBaseUrl(baseUrl);
        if ("openai".equalsIgnoreCase(providerCode) && OPENAI_PUBLIC_BASE_URL.equalsIgnoreCase(normalizedBaseUrl)) {
            return StrUtil.blankToDefault(openAiProxyBaseUrl, DEFAULT_OPENAI_PROXY_BASE_URL);
        }
        return normalizedBaseUrl;
    }

    public static String resolveActualRequestBaseUrl(String providerCode, String baseUrl) {
        return resolveActualRequestBaseUrl(providerCode, baseUrl, DEFAULT_OPENAI_PROXY_BASE_URL);
    }

    /**
     * 解析Extra请求头。
     */
    private MultiValueMap<String, String> parseExtraHeaders(String extraHeadersJson) {
        LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        if (StrUtil.isBlank(extraHeadersJson)) {
            return headers;
        }

        try {
            Map<String, Object> values = objectMapper.readValue(
                    extraHeadersJson,
                    new TypeReference<Map<String, Object>>() {
                    }
            );
            values.forEach((key, value) -> {
                if (StrUtil.isNotBlank(key) && value != null) {
                    headers.add(key.trim(), String.valueOf(value));
                }
            });
        } catch (Exception e) {
            log.warn("解析额外请求头失败，将忽略该配置: {}", e.getMessage());
        }
        return headers;
    }

    /**
     * 解析运行时配置。
     */
    private AiRuntimeConfig resolveRuntimeConfig(Map<String, String> configs) {
        Map<String, SavedProviderConfigSnapshot> savedProviderConfigs = loadSavedProviderConfigs(configs);
        AiMode requestedMode = AiMode.resolve(configs.get(AI_MODE_KEY));
        SavedProviderConfigSnapshot selectedSavedProvider = resolveSelectedSavedProvider(configs, savedProviderConfigs);
        if (requestedMode == AiMode.CUSTOM && selectedSavedProvider != null) {
            AiProviderDescriptor descriptor = AiProviderRegistry.resolve(
                    selectedSavedProvider.providerCode(),
                    selectedSavedProvider.apiUrl()
            );
            return new AiRuntimeConfig(
                    selectedSavedProvider.providerCode(),
                    selectedSavedProvider.apiUrl(),
                    selectedSavedProvider.apiKey(),
                    selectedSavedProvider.model(),
                    selectedSavedProvider.temperature(),
                    selectedSavedProvider.maxTokens(),
                    selectedSavedProvider.extraHeadersJson(),
                    descriptor.resolveCapabilities(selectedSavedProvider.model()),
                    AiMode.CUSTOM
            );
        }

        // 自定义模式缺少可用快照时统一回退到赠送模式，保证新租户或脏配置场景下仍能得到可工作的默认模型。
        return resolveGiftRuntimeConfig();
    }

    /**
     * 解析指定模型运行时配置。
     */
    private AiRuntimeConfig resolveRuntimeConfigForSelection(Map<String, String> configs,
                                                             String providerCode,
                                                             String modelName) {
        return resolveRuntimeConfigForSelection(configs, null, providerCode, modelName);
    }

    private AiRuntimeConfig resolveRuntimeConfigForSelection(Map<String, String> configs,
                                                             String modelSource,
                                                             String providerCode,
                                                             String modelName) {
        String normalizedSource = AiModelSource.normalize(modelSource);
        if (!AiModelSource.isKnown(normalizedSource)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "未知的模型来源");
        }
        if (AiModelSource.isCustom(normalizedSource)) {
            return resolveCustomRuntimeConfig(configs, providerCode, modelName);
        }
        if (AiModelSource.isSystem(normalizedSource)) {
            AiRuntimeConfig runtimeConfig = resolveSystemOrGiftRuntimeConfig(providerCode, modelName);
            if (runtimeConfig == null) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "当前系统模型服务商未配置，无法切换模型");
            }
            return runtimeConfig;
        }

        String rawRequestedProvider = StrUtil.nullToEmpty(providerCode).trim();
        String requestedProvider = StrUtil.isBlank(rawRequestedProvider)
                ? ""
                : AiProviderRegistry.resolve(rawRequestedProvider, null).getCode();
        String requestedModel = StrUtil.nullToEmpty(modelName).trim();
        AiRuntimeConfig currentRuntimeConfig = resolveRuntimeConfig(configs);

        if (StrUtil.isBlank(requestedProvider)) {
            requestedProvider = currentRuntimeConfig.providerCode();
        }
        if (StrUtil.isBlank(requestedModel)) {
            requestedModel = currentRuntimeConfig.model();
        }

        SavedProviderConfigSnapshot savedProvider = loadSavedProviderConfigs(configs).get(requestedProvider);
        if (savedProvider != null) {
            AiProviderDescriptor descriptor = AiProviderRegistry.resolve(savedProvider.providerCode(), savedProvider.apiUrl());
            return new AiRuntimeConfig(
                    savedProvider.providerCode(),
                    savedProvider.apiUrl(),
                    savedProvider.apiKey(),
                    requestedModel,
                    savedProvider.temperature(),
                    savedProvider.maxTokens(),
                    savedProvider.extraHeadersJson(),
                    descriptor.resolveCapabilities(requestedModel),
                    AiMode.CUSTOM
            );
        }

        AiRuntimeConfig systemRuntimeConfig = resolveSystemRuntimeConfig(requestedProvider, requestedModel);
        if (systemRuntimeConfig != null) {
            return systemRuntimeConfig;
        }

        AiRuntimeConfig giftRuntimeConfig = resolveGiftRuntimeConfig();
        if (Objects.equals(giftRuntimeConfig.providerCode(), requestedProvider)) {
            AiProviderDescriptor descriptor = AiProviderRegistry.resolve(giftRuntimeConfig.providerCode(), giftRuntimeConfig.apiUrl());
            return new AiRuntimeConfig(
                    giftRuntimeConfig.providerCode(),
                    giftRuntimeConfig.apiUrl(),
                    giftRuntimeConfig.apiKey(),
                    requestedModel,
                    giftRuntimeConfig.temperature(),
                    giftRuntimeConfig.maxTokens(),
                    giftRuntimeConfig.extraHeadersJson(),
                    descriptor.resolveCapabilities(requestedModel),
                    giftRuntimeConfig.mode()
            );
        }

        if (Objects.equals(currentRuntimeConfig.providerCode(), requestedProvider)) {
            AiProviderDescriptor descriptor = AiProviderRegistry.resolve(currentRuntimeConfig.providerCode(), currentRuntimeConfig.apiUrl());
            return new AiRuntimeConfig(
                    currentRuntimeConfig.providerCode(),
                    currentRuntimeConfig.apiUrl(),
                    currentRuntimeConfig.apiKey(),
                    requestedModel,
                    currentRuntimeConfig.temperature(),
                    currentRuntimeConfig.maxTokens(),
                    currentRuntimeConfig.extraHeadersJson(),
                    descriptor.resolveCapabilities(requestedModel),
                    currentRuntimeConfig.mode()
            );
        }

        throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "当前模型服务商未配置，无法切换模型");
    }

    private AiRuntimeConfig resolveCustomRuntimeConfig(Map<String, String> configs, String providerCode, String modelName) {
        String requestedProvider = StrUtil.isBlank(providerCode)
                ? ""
                : AiProviderRegistry.resolve(providerCode, null).getCode();
        String requestedModel = StrUtil.nullToEmpty(modelName).trim();
        if (StrUtil.hasBlank(requestedProvider, requestedModel)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "请选择可用的自定义 AI 模型");
        }

        SavedProviderConfigSnapshot savedProvider = loadSavedProviderConfigs(configs).get(requestedProvider);
        if (savedProvider == null || StrUtil.isBlank(savedProvider.apiKey())
                || !requestedModel.equals(savedProvider.model())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "当前自定义模型未配置，无法切换模型");
        }

        AiProviderDescriptor descriptor = AiProviderRegistry.resolve(savedProvider.providerCode(), savedProvider.apiUrl());
        return new AiRuntimeConfig(
                savedProvider.providerCode(),
                savedProvider.apiUrl(),
                savedProvider.apiKey(),
                savedProvider.model(),
                savedProvider.temperature(),
                savedProvider.maxTokens(),
                savedProvider.extraHeadersJson(),
                descriptor.resolveCapabilities(savedProvider.model()),
                AiMode.CUSTOM
        );
    }

    private AiRuntimeConfig resolveSystemOrGiftRuntimeConfig(String providerCode, String modelName) {
        String requestedProvider = StrUtil.isBlank(providerCode)
                ? ""
                : AiProviderRegistry.resolve(providerCode, null).getCode();
        String requestedModel = StrUtil.nullToEmpty(modelName).trim();
        if (StrUtil.isBlank(requestedProvider)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "请选择可用的系统 AI 模型");
        }

        AiRuntimeConfig systemRuntimeConfig = resolveSystemRuntimeConfig(requestedProvider, requestedModel);
        if (systemRuntimeConfig != null) {
            return systemRuntimeConfig;
        }

        AiRuntimeConfig giftRuntimeConfig = resolveGiftRuntimeConfig();
        if (!Objects.equals(giftRuntimeConfig.providerCode(), requestedProvider)
                || StrUtil.isBlank(giftRuntimeConfig.apiKey())) {
            return null;
        }

        AiProviderDescriptor descriptor = AiProviderRegistry.resolve(giftRuntimeConfig.providerCode(), giftRuntimeConfig.apiUrl());
        String model = StrUtil.blankToDefault(requestedModel, giftRuntimeConfig.model());
        return new AiRuntimeConfig(
                giftRuntimeConfig.providerCode(),
                giftRuntimeConfig.apiUrl(),
                giftRuntimeConfig.apiKey(),
                model,
                giftRuntimeConfig.temperature(),
                giftRuntimeConfig.maxTokens(),
                giftRuntimeConfig.extraHeadersJson(),
                descriptor.resolveCapabilities(model),
                giftRuntimeConfig.mode()
        );
    }

    private AiRuntimeConfig resolveSystemRuntimeConfig(String providerCode, String requestedModel) {
        SystemProviderSnapshot snapshot = loadSystemProviderSnapshots().get(providerCode);
        if (snapshot == null || StrUtil.isBlank(snapshot.apiKey())) {
            return null;
        }

        String model = StrUtil.blankToDefault(StrUtil.trim(requestedModel), snapshot.model());
        AiProviderDescriptor descriptor = AiProviderRegistry.resolve(snapshot.providerCode(), snapshot.apiUrl());
        return new AiRuntimeConfig(
                descriptor.getCode(),
                snapshot.apiUrl(),
                snapshot.apiKey(),
                model,
                snapshot.temperature(),
                snapshot.maxTokens(),
                snapshot.extraHeadersJson(),
                descriptor.resolveCapabilities(model),
                AiMode.GIFT
        );
    }

    private Map<String, SystemProviderSnapshot> loadSystemProviderSnapshots() {
        if (systemAiModelProperties == null || systemAiModelProperties.getProviders() == null
                || systemAiModelProperties.getProviders().isEmpty()) {
            return Map.of();
        }

        Map<String, SystemProviderSnapshot> snapshots = new LinkedHashMap<>();
        systemAiModelProperties.getProviders().forEach((configuredProviderCode, provider) -> {
            if (provider == null || Boolean.FALSE.equals(provider.getEnabled())
                    || StrUtil.isBlank(provider.getApiKey())) {
                return;
            }

            String normalizedBaseUrl = normalizeCompatibleBaseUrl(provider.getBaseUrl());
            AiProviderDescriptor descriptor = AiProviderRegistry.resolve(configuredProviderCode, normalizedBaseUrl);
            String apiUrl = normalizeCompatibleBaseUrl(StrUtil.blankToDefault(normalizedBaseUrl, descriptor.getBaseUrl()));
            if (StrUtil.isBlank(apiUrl)) {
                return;
            }

            String model = StrUtil.blankToDefault(StrUtil.trim(provider.getModel()), resolveDefaultModel(descriptor));
            snapshots.put(descriptor.getCode(), new SystemProviderSnapshot(
                    descriptor.getCode(),
                    apiUrl,
                    provider.getApiKey().trim(),
                    model,
                    provider.getTemperature() != null ? provider.getTemperature() : defaultTemperature,
                    provider.getMaxTokens() != null ? provider.getMaxTokens() : defaultMaxTokens,
                    StrUtil.blankToDefault(provider.getExtraHeadersJson(), null)
            ));
        });
        return snapshots;
    }

    private String resolveDefaultModel(AiProviderDescriptor descriptor) {
        if (descriptor != null && descriptor.getRecommendedModels() != null && !descriptor.getRecommendedModels().isEmpty()) {
            return descriptor.getRecommendedModels().get(0);
        }
        return defaultModel;
    }

    /**
     * 解析赠送模式运行时配置。
     */
    private AiRuntimeConfig resolveGiftRuntimeConfig() {
        String resolvedApiUrl = normalizeCompatibleBaseUrl(StrUtil.blankToDefault(giftBaseUrl, defaultBaseUrl));
        AiProviderDescriptor descriptor = AiProviderRegistry.resolve(null, resolvedApiUrl);
        String resolvedApiKey = StrUtil.blankToDefault(giftApiKey, defaultApiKey);
        String resolvedModel = StrUtil.blankToDefault(giftModel, defaultModel);
        return new AiRuntimeConfig(
                descriptor.getCode(),
                resolvedApiUrl,
                resolvedApiKey,
                resolvedModel,
                defaultTemperature,
                defaultMaxTokens,
                null,
                descriptor.resolveCapabilities(resolvedModel),
                AiMode.GIFT
        );
    }

    /**
     * 构建指定模型 ChatClient 缓存 Key。
     */
    private String buildSelectedClientCacheKey(AiRuntimeConfig runtimeConfig, String appCode) {
        Long tenantId = TenantContextHolder.getTenantId();
        long tenantKey = tenantId != null ? tenantId : 0L;
        return tenantKey
                + "|" + chatApplicationRegistry.normalize(appCode)
                + "|" + runtimeConfig.providerCode()
                + "|" + runtimeConfig.apiUrl()
                + "|" + runtimeConfig.model()
                + "|" + Objects.hashCode(runtimeConfig.apiKey())
                + "|" + runtimeConfig.maxTokens()
                + "|" + runtimeConfig.temperature()
                + "|" + Objects.hashCode(runtimeConfig.extraHeadersJson())
                + "|" + (runtimeConfig.capabilities() != null && runtimeConfig.capabilities().isSupportsToolCall());
    }

    /**
     * 清理当前租户的指定模型客户端缓存。
     */
    private void evictSelectedModelChatClients(long tenantKey) {
        String prefix = tenantKey + "|";
        selectedModelChatClients.keySet().removeIf(key -> key.startsWith(prefix));
    }

    /**
     * 判断是否存在已保存自定义配置。
     */
    private boolean hasSavedCustomConfig(Map<String, String> configs) {
        return !loadSavedProviderConfigs(configs).isEmpty();
    }

    /**
     * 加载已保存服务商配置。
     */
    private Map<String, SavedProviderConfigSnapshot> loadSavedProviderConfigs(Map<String, String> configs) {
        Map<String, SavedProviderConfigSnapshot> savedConfigs = new ConcurrentHashMap<>();
        parseStoredProviderConfigs(configs.get(AI_PROVIDER_CONFIGS_KEY)).forEach((providerCode, storedConfig) -> {
            SavedProviderConfigSnapshot snapshot = toSavedProviderConfigSnapshot(providerCode, storedConfig);
            if (snapshot != null) {
                savedConfigs.put(snapshot.providerCode(), snapshot);
            }
        });

        // 兼容旧版单服务商字段，避免升级后历史租户因为尚未迁移 ai_provider_configs 而失去可用配置。
        SavedProviderConfigSnapshot legacySnapshot = buildLegacyProviderSnapshot(configs);
        if (legacySnapshot != null) {
            savedConfigs.putIfAbsent(legacySnapshot.providerCode(), legacySnapshot);
        }
        return savedConfigs;
    }

    /**
     * 解析Selected已保存服务商。
     */
    private SavedProviderConfigSnapshot resolveSelectedSavedProvider(Map<String, String> configs,
                                                                    Map<String, SavedProviderConfigSnapshot> savedProviderConfigs) {
        if (savedProviderConfigs.isEmpty()) {
            return null;
        }

        String activeProviderCode = StrUtil.nullToEmpty(configs.get(AI_PROVIDER_KEY)).trim().toLowerCase();
        if (StrUtil.isNotBlank(activeProviderCode)) {
            SavedProviderConfigSnapshot activeConfig = savedProviderConfigs.get(activeProviderCode);
            if (activeConfig != null) {
                return activeConfig;
            }
        }

        return savedProviderConfigs.values().stream().findFirst().orElse(null);
    }

    /**
     * 解析已存储服务商配置。
     */
    private Map<String, StoredProviderConfig> parseStoredProviderConfigs(String providerConfigsJson) {
        if (StrUtil.isBlank(providerConfigsJson)) {
            return Map.of();
        }

        try {
            Map<String, StoredProviderConfig> storedConfigs = objectMapper.readValue(
                    providerConfigsJson,
                    new TypeReference<Map<String, StoredProviderConfig>>() {
                    }
            );
            return storedConfigs != null ? storedConfigs : Map.of();
        } catch (Exception e) {
            log.warn("解析多服务商 AI 配置失败，将回退到旧版单配置: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * 构建旧版服务商快照。
     */
    private SavedProviderConfigSnapshot buildLegacyProviderSnapshot(Map<String, String> configs) {
        String apiUrl = normalizeCompatibleBaseUrl(configs.get(AI_API_URL_KEY));
        String apiKey = StrUtil.nullToEmpty(configs.get(AI_API_KEY_KEY)).trim();
        String model = StrUtil.nullToEmpty(configs.get(AI_MODEL_KEY)).trim();
        if (StrUtil.hasBlank(apiUrl, apiKey, model)) {
            return null;
        }

        String providerCode = AiProviderRegistry.resolve(configs.get(AI_PROVIDER_KEY), apiUrl).getCode();
        return new SavedProviderConfigSnapshot(
                providerCode,
                apiUrl,
                apiKey,
                model,
                parseDouble(configs.get(AI_TEMPERATURE_KEY), defaultTemperature),
                parseInt(configs.get(AI_MAX_TOKENS_KEY), defaultMaxTokens),
                StrUtil.blankToDefault(configs.get(AI_EXTRA_HEADERS_KEY), null)
        );
    }

    /**
     * 转换为已保存服务商配置快照。
     */
    private SavedProviderConfigSnapshot toSavedProviderConfigSnapshot(String fallbackProviderCode, StoredProviderConfig storedConfig) {
        if (storedConfig == null) {
            return null;
        }

        String apiUrl = normalizeCompatibleBaseUrl(storedConfig.apiUrl());
        String apiKey = StrUtil.nullToEmpty(storedConfig.apiKey()).trim();
        String model = StrUtil.nullToEmpty(storedConfig.model()).trim();
        if (StrUtil.hasBlank(apiUrl, apiKey, model)) {
            return null;
        }

        String providerCode = AiProviderRegistry.resolve(
                StrUtil.blankToDefault(storedConfig.provider(), fallbackProviderCode),
                apiUrl
        ).getCode();

        return new SavedProviderConfigSnapshot(
                providerCode,
                apiUrl,
                apiKey,
                model,
                storedConfig.temperature() != null ? storedConfig.temperature() : defaultTemperature,
                storedConfig.maxTokens() != null ? storedConfig.maxTokens() : defaultMaxTokens,
                StrUtil.blankToDefault(storedConfig.extraHeadersJson(), null)
        );
    }

    /**
     * 加载AI配置数据库。
     */
    private Map<String, String> loadAiConfigsFromDB() {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigType, "ai");

        List<SystemConfig> configs = systemConfigMapper.selectList(wrapper);

        return configs.stream()
                .collect(Collectors.toMap(
                        SystemConfig::getConfigKey,
                        c -> c.getConfigValue() != null ? c.getConfigValue() : "",
                        (v1, v2) -> v2
                ));
    }

    /**
     * 解析Double。
     */
    private double parseDouble(String value, double defaultValue) {
        if (StrUtil.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 解析Int。
     */
    private int parseInt(String value, int defaultValue) {
        if (StrUtil.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean supportsParallelToolCalls(String providerCode, String baseUrl) {
        String resolvedProviderCode = AiProviderRegistry.resolve(providerCode, baseUrl).getCode();
        return "openai".equals(resolvedProviderCode)
                || "dashscope".equals(resolvedProviderCode);
    }

    boolean shouldEnableParallelToolCalls(boolean toolCallingEnabled, Object[] defaultTools,
                                         String providerCode, String baseUrl) {
        return toolCallingEnabled
                && defaultTools != null
                && defaultTools.length > 0
                && supportsParallelToolCalls(providerCode, baseUrl);
    }

    /**
     * 生成默认能力。
     */
    private AiModelCapabilities defaultCapabilities() {
        return AiModelCapabilities.builder()
                .supportsStream(true)
                .supportsToolCall(true)
                .supportsVision(false)
                .supportsAudioTranscription(false)
                .build();
    }

    /**
     * 标准化Compatible基础地址。
     */
    public static String normalizeCompatibleBaseUrl(String baseUrl) {
        if (StrUtil.isBlank(baseUrl)) {
            return baseUrl;
        }

        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.matches("(?i).*/v1$")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        }

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * 初始化动态聊天客户端。
     */
    @PostConstruct
    public void init() {
        try {
            refreshChatClient();
        } catch (Exception e) {
            log.warn("初始化 ChatClient 失败（可能配置尚未准备好）: {}", e.getMessage());
        }
    }

    private record AiRuntimeConfig(
            String providerCode,
            String apiUrl,
            String apiKey,
            String model,
            Double temperature,
            Integer maxTokens,
            String extraHeadersJson,
            AiModelCapabilities capabilities,
            AiMode mode
    ) {
    }

    public record AiRuntimeConfigSnapshot(
            String providerCode,
            String apiUrl,
            String apiKey,
            String model,
            String extraHeadersJson,
            AiModelCapabilities capabilities,
            AiMode mode
    ) {
    }

    public record ConfiguredModelSnapshot(
            String providerCode,
            String providerLabel,
            String model
    ) {
    }

    private record StoredProviderConfig(
            String provider,
            String apiUrl,
            String apiKey,
            String model,
            Double temperature,
            Integer maxTokens,
            String extraHeadersJson
    ) {
    }

    private record SavedProviderConfigSnapshot(
            String providerCode,
            String apiUrl,
            String apiKey,
            String model,
            Double temperature,
            Integer maxTokens,
            String extraHeadersJson
    ) {
    }

    private record SystemProviderSnapshot(
            String providerCode,
            String apiUrl,
            String apiKey,
            String model,
            Double temperature,
            Integer maxTokens,
            String extraHeadersJson
    ) {
    }
}

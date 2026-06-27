package com.kakarote.ai_crm.ai;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.ai.app.ChatApplicationCodes;
import com.kakarote.ai_crm.ai.app.ChatApplicationDefinition;
import com.kakarote.ai_crm.ai.app.ChatApplicationRegistry;
import com.kakarote.ai_crm.ai.provider.AiModelCapabilities;
import com.kakarote.ai_crm.ai.provider.AiProviderDescriptor;
import com.kakarote.ai_crm.ai.provider.AiProviderRegistry;
import com.kakarote.ai_crm.ai.tools.ContactTools;
import com.kakarote.ai_crm.ai.tools.CrmNoopTools;
import com.kakarote.ai_crm.ai.tools.CustomerTools;
import com.kakarote.ai_crm.ai.tools.CandidateTools;
import com.kakarote.ai_crm.ai.tools.FollowupTools;
import com.kakarote.ai_crm.ai.tools.KnowledgeTools;
import com.kakarote.ai_crm.ai.tools.MailTools;
import com.kakarote.ai_crm.ai.tools.ProductTools;
import com.kakarote.ai_crm.ai.tools.ProjectTools;
import com.kakarote.ai_crm.ai.tools.RelationTools;
import com.kakarote.ai_crm.ai.tools.ScheduleTools;
import com.kakarote.ai_crm.ai.tools.TaskTools;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 动态 ChatClient 提供者。
 */
@Slf4j
@Component
public class DynamicChatClientProvider {

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

    private volatile ChatClient currentChatClient;
    private final Object lock = new Object();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Autowired
    private ChatApplicationRegistry chatApplicationRegistry;

    @Autowired
    private CustomerTools customerTools;

    @Autowired
    private TaskTools taskTools;

    @Autowired
    private ProjectTools projectTools;

    @Autowired
    private ProductTools productTools;

    @Autowired
    private CandidateTools candidateTools;

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
    private CrmNoopTools crmNoopTools;

    @Autowired
    private ToolCallingManager toolCallingManager;

    @Autowired(required = false)
    private ObservationRegistry observationRegistry;

    @Value("${spring.ai.openai.base-url:https://dashscope.aliyuncs.com/compatible-mode}")
    private String defaultBaseUrl;

    @Value("${spring.ai.openai.chat.options.model:qwen3.6-plus}")
    private String defaultModel;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double defaultTemperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:2048}")
    private Integer defaultMaxTokens;

    @Value("${system-ai.openai.proxy-base-url:" + DEFAULT_OPENAI_PROXY_BASE_URL + "}")
    private String openAiProxyBaseUrl;

    public ChatClient getChatClient() {
        ChatClient client = currentChatClient;
        if (client == null) {
            synchronized (lock) {
                client = currentChatClient;
                if (client == null) {
                    refreshChatClient();
                    client = currentChatClient;
                }
            }
        }
        return client;
    }

    public ChatClient getChatClient(String providerCode, String modelName) {
        if (StrUtil.isBlank(providerCode) && StrUtil.isBlank(modelName)) {
            return getChatClient();
        }
        AiRuntimeConfig runtimeConfig = resolveRuntimeConfigForSelection(loadAiConfigsFromDB(), providerCode, modelName);
        return createChatClient(runtimeConfig);
    }

    public ChatClient getChatClient(String providerCode, String modelName, String appCode) {
        if (StrUtil.isBlank(appCode)) {
            return getChatClient(providerCode, modelName);
        }
        AiRuntimeConfig runtimeConfig = StrUtil.isBlank(providerCode) && StrUtil.isBlank(modelName)
                ? resolveRuntimeConfig(loadAiConfigsFromDB())
                : resolveRuntimeConfigForSelection(loadAiConfigsFromDB(), providerCode, modelName);
        return createChatClient(runtimeConfig, appCode);
    }

    public void refreshChatClient() {
        synchronized (lock) {
            AiRuntimeConfig runtimeConfig = resolveRuntimeConfig(loadAiConfigsFromDB());

            if (StrUtil.isBlank(runtimeConfig.apiKey())) {
                log.warn("AI 运行 Key 未配置，mode={}", runtimeConfig.mode().getCode());
            }

            currentChatClient = createChatClient(runtimeConfig);
            log.info(
                    "ChatClient 刷新完成: mode={}, provider={}, baseUrl={}, model={}",
                    runtimeConfig.mode().getCode(),
                    runtimeConfig.providerCode(),
                    runtimeConfig.apiUrl(),
                    runtimeConfig.model()
            );
        }
    }

    public boolean isApiKeyConfigured() {
        return StrUtil.isNotBlank(resolveRuntimeConfig(loadAiConfigsFromDB()).apiKey());
    }

    public AiModelCapabilities getCurrentCapabilities() {
        return resolveRuntimeConfig(loadAiConfigsFromDB()).capabilities();
    }

    public AiRuntimeConfigSnapshot getCurrentRuntimeConfigSnapshot() {
        AiRuntimeConfig runtimeConfig = resolveRuntimeConfig(loadAiConfigsFromDB());
        return toSnapshot(runtimeConfig);
    }

    public AiRuntimeConfigSnapshot getRuntimeConfigSnapshot(String providerCode, String modelName) {
        AiRuntimeConfig runtimeConfig = StrUtil.isBlank(providerCode) && StrUtil.isBlank(modelName)
                ? resolveRuntimeConfig(loadAiConfigsFromDB())
                : resolveRuntimeConfigForSelection(loadAiConfigsFromDB(), providerCode, modelName);
        return toSnapshot(runtimeConfig);
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

    public AiMode getCurrentMode() {
        return resolveRuntimeConfig(loadAiConfigsFromDB()).mode();
    }

    public void evictChatClient() {
        currentChatClient = null;
    }

    public ChatClient createChatClient(String baseUrl, String apiKey, String model,
                                       Double temperature, Integer maxTokens) {
        String normalizedBaseUrl = normalizeCompatibleBaseUrl(baseUrl);
        String providerCode = AiProviderRegistry.resolve(null, normalizedBaseUrl).getCode();
        return createChatClient(
                providerCode,
                normalizedBaseUrl,
                apiKey,
                model,
                temperature,
                maxTokens,
                null,
                defaultCapabilities(),
                true
        );
    }

    public ChatClient createChatClient(String providerCode, String baseUrl, String apiKey, String model,
                                       Double temperature, Integer maxTokens,
                                       String extraHeadersJson, AiModelCapabilities capabilities,
                                       boolean registerTools) {
        return createChatClient(
                providerCode,
                baseUrl,
                apiKey,
                model,
                temperature,
                maxTokens,
                extraHeadersJson,
                capabilities,
                registerTools,
                null
        );
    }

    private ChatClient createChatClient(String providerCode, String baseUrl, String apiKey, String model,
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
                ? observationRegistry
                : ObservationRegistry.NOOP;
        OpenAiChatModel chatModel = new OpenAiChatModel(
                openAiApi,
                options,
                toolCallingManager,
                RetryTemplate.builder().build(),
                obsRegistry
        );

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

        Map<String, String> httpHeaders = buildChatHttpHeaders(providerCode, baseUrl);
        if (!httpHeaders.isEmpty()) {
            builder.httpHeaders(httpHeaders);
        }

        Map<String, Object> extraBody = new LinkedHashMap<>();
        applyThinkingDisabledOptions(extraBody, providerCode, baseUrl, resolvedModel);
        if (!extraBody.isEmpty()) {
            builder.extraBody(extraBody);
        }

        return builder.build();
    }

    private Map<String, String> buildChatHttpHeaders(String providerCode, String baseUrl) {
        if (shouldRequestEventStream(providerCode, baseUrl)) {
            return Map.of(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE);
        }
        return Map.of();
    }

    private boolean shouldRequestEventStream(String providerCode, String baseUrl) {
        String resolvedProviderCode = AiProviderRegistry.resolve(providerCode, normalizeCompatibleBaseUrl(baseUrl)).getCode();
        return "wukong_external".equals(resolvedProviderCode);
    }

    private Object[] resolveDefaultTools() {
        return java.util.Arrays.stream(new Object[] {
                customerTools, taskTools, projectTools, productTools, candidateTools, knowledgeTools,
                contactTools, followupTools, scheduleTools, relationTools, mailTools,
                crmNoopTools
        }).filter(Objects::nonNull).toArray();
    }

    Object[] resolveDefaultTools(String appCode) {
        if (StrUtil.isBlank(appCode)) {
            return resolveDefaultTools();
        }
        ChatApplicationDefinition application = chatApplicationRegistry == null
                ? null
                : chatApplicationRegistry.resolve(appCode);
        if (application == null || ChatApplicationCodes.GENERAL.equals(application.code())) {
            return new Object[0];
        }

        List<Object> tools = new ArrayList<>();
        for (String group : application.toolGroups()) {
            addToolsForGroup(tools, group);
        }
        return tools.toArray();
    }

    private void addToolsForGroup(List<Object> tools, String group) {
        switch (StrUtil.nullToEmpty(group)) {
            case ChatApplicationRegistry.TOOL_GROUP_CUSTOMER -> addTool(tools, customerTools);
            case ChatApplicationRegistry.TOOL_GROUP_CONTACT -> addTool(tools, contactTools);
            case ChatApplicationRegistry.TOOL_GROUP_FOLLOW_UP -> addTool(tools, followupTools);
            case ChatApplicationRegistry.TOOL_GROUP_TASK_SCHEDULE -> {
                addTool(tools, taskTools);
                addTool(tools, scheduleTools);
            }
            case ChatApplicationRegistry.TOOL_GROUP_MAIL -> addTool(tools, mailTools);
            case ChatApplicationRegistry.TOOL_GROUP_PRODUCT -> addTool(tools, productTools);
            case ChatApplicationRegistry.TOOL_GROUP_PROJECT -> addTool(tools, projectTools);
            case ChatApplicationRegistry.TOOL_GROUP_RELATION -> addTool(tools, relationTools);
            case ChatApplicationRegistry.TOOL_GROUP_CANDIDATE -> addTool(tools, candidateTools);
            case ChatApplicationRegistry.TOOL_GROUP_KNOWLEDGE -> addTool(tools, knowledgeTools);
            case ChatApplicationRegistry.TOOL_GROUP_CRM_NOOP -> addTool(tools, crmNoopTools);
            default -> {
            }
        }
    }

    private void addTool(List<Object> tools, Object tool) {
        if (tool != null && !tools.contains(tool)) {
            tools.add(tool);
        }
    }

    boolean shouldEnableParallelToolCalls(boolean toolCallingEnabled, Object[] defaultTools,
                                          String providerCode, String baseUrl) {
        return toolCallingEnabled
                && defaultTools != null
                && defaultTools.length > 0
                && supportsParallelToolCalls(providerCode, baseUrl);
    }

    private boolean supportsParallelToolCalls(String providerCode, String baseUrl) {
        String normalizedBaseUrl = normalizeCompatibleBaseUrl(baseUrl);
        String resolvedProviderCode = AiProviderRegistry.resolve(providerCode, normalizedBaseUrl).getCode();
        return "openai".equals(resolvedProviderCode)
                || "dashscope".equals(resolvedProviderCode)
                || "wukong_external".equals(resolvedProviderCode);
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

    private boolean shouldDisableThinkingObject(String providerCode, String baseUrl, String model) {
        String normalizedModel = normalizeForMatching(model);
        return shouldDisableDeepSeekThinking(providerCode, baseUrl)
                || isDeepSeekThinkingModel(normalizedModel)
                || isKimiK2SwitchableThinkingModel(normalizedModel)
                || isDoubaoThinkingModel(normalizedModel)
                || isGlmThinkingModel(normalizedModel)
                || isHunyuanThinkingModel(normalizedModel);
    }

    private boolean shouldDisableDeepSeekThinking(String providerCode, String baseUrl) {
        if ("deepseek".equals(normalizeProviderCodeForThinking(providerCode))) {
            return true;
        }
        String normalizedBaseUrl = normalizeForMatching(baseUrl);
        return normalizedBaseUrl.contains("api.deepseek.com") || normalizedBaseUrl.contains("deepseek.com");
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

    private ChatClient createChatClient(AiRuntimeConfig runtimeConfig) {
        return createChatClient(runtimeConfig, null);
    }

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

    private OpenAiApi buildOpenAiApi(String providerCode, String baseUrl, String apiKey, String extraHeadersJson) {
        String normalizedBaseUrl = normalizeCompatibleBaseUrl(baseUrl);
        AiProviderDescriptor descriptor = AiProviderRegistry.resolve(providerCode, normalizedBaseUrl);
        String actualRequestBaseUrl = resolveActualRequestBaseUrl(descriptor.getCode(), normalizedBaseUrl, openAiProxyBaseUrl);
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

    public static String resolveActualRequestBaseUrl(String providerCode, String baseUrl) {
        return resolveActualRequestBaseUrl(providerCode, baseUrl, DEFAULT_OPENAI_PROXY_BASE_URL);
    }

    public static String resolveActualRequestBaseUrl(String providerCode, String baseUrl, String openAiProxyBaseUrl) {
        String normalizedBaseUrl = normalizeCompatibleBaseUrl(baseUrl);
        if ("openai".equalsIgnoreCase(providerCode) && OPENAI_PUBLIC_BASE_URL.equalsIgnoreCase(normalizedBaseUrl)) {
            return StrUtil.blankToDefault(openAiProxyBaseUrl, DEFAULT_OPENAI_PROXY_BASE_URL);
        }
        return normalizedBaseUrl;
    }

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

    private AiRuntimeConfig resolveRuntimeConfig(Map<String, String> configs) {
        Map<String, SavedProviderConfigSnapshot> savedProviderConfigs = loadSavedProviderConfigs(configs);
        SavedProviderConfigSnapshot selectedSavedProvider = resolveSelectedSavedProvider(configs, savedProviderConfigs);
        if (selectedSavedProvider != null) {
            return toRuntimeConfig(selectedSavedProvider, null);
        }

        String resolvedApiUrl = normalizeCompatibleBaseUrl(
                StrUtil.blankToDefault(configs.get(AI_API_URL_KEY), defaultBaseUrl)
        );
        AiProviderDescriptor descriptor = AiProviderRegistry.resolve(configs.get(AI_PROVIDER_KEY), resolvedApiUrl);
        String resolvedModel = StrUtil.blankToDefault(configs.get(AI_MODEL_KEY), defaultModel);
        return new AiRuntimeConfig(
                descriptor.getCode(),
                resolvedApiUrl,
                "",
                resolvedModel,
                defaultTemperature,
                defaultMaxTokens,
                null,
                descriptor.resolveCapabilities(resolvedModel),
                AiMode.CUSTOM
        );
    }

    private AiRuntimeConfig resolveRuntimeConfigForSelection(Map<String, String> configs,
                                                            String providerCode,
                                                            String modelName) {
        Map<String, SavedProviderConfigSnapshot> savedProviderConfigs = loadSavedProviderConfigs(configs);
        if (savedProviderConfigs.isEmpty()) {
            return resolveRuntimeConfig(configs);
        }

        String normalizedProvider = StrUtil.nullToEmpty(providerCode).trim().toLowerCase();
        String requestedModel = StrUtil.nullToEmpty(modelName).trim();
        SavedProviderConfigSnapshot snapshot = null;

        if (StrUtil.isNotBlank(normalizedProvider)) {
            snapshot = savedProviderConfigs.get(normalizedProvider);
        }
        if (snapshot == null && StrUtil.isNotBlank(requestedModel)) {
            snapshot = savedProviderConfigs.values().stream()
                    .filter(item -> requestedModel.equalsIgnoreCase(item.model()))
                    .findFirst()
                    .orElse(null);
        }
        if (snapshot == null) {
            snapshot = resolveSelectedSavedProvider(configs, savedProviderConfigs);
        }
        return snapshot != null ? toRuntimeConfig(snapshot, requestedModel) : resolveRuntimeConfig(configs);
    }

    private AiRuntimeConfig toRuntimeConfig(SavedProviderConfigSnapshot snapshot, String requestedModel) {
        String model = StrUtil.blankToDefault(StrUtil.nullToEmpty(requestedModel).trim(), snapshot.model());
        AiProviderDescriptor descriptor = AiProviderRegistry.resolve(
                snapshot.providerCode(),
                snapshot.apiUrl()
        );
        return new AiRuntimeConfig(
                snapshot.providerCode(),
                snapshot.apiUrl(),
                snapshot.apiKey(),
                model,
                snapshot.temperature(),
                snapshot.maxTokens(),
                snapshot.extraHeadersJson(),
                descriptor.resolveCapabilities(model),
                AiMode.CUSTOM
        );
    }

    private Map<String, SavedProviderConfigSnapshot> loadSavedProviderConfigs(Map<String, String> configs) {
        Map<String, SavedProviderConfigSnapshot> savedConfigs = new java.util.HashMap<>();
        parseStoredProviderConfigs(configs.get(AI_PROVIDER_CONFIGS_KEY)).forEach((providerCode, storedConfig) -> {
            SavedProviderConfigSnapshot snapshot = toSavedProviderConfigSnapshot(providerCode, storedConfig);
            if (snapshot != null) {
                savedConfigs.put(snapshot.providerCode(), snapshot);
            }
        });

        SavedProviderConfigSnapshot legacySnapshot = buildLegacyProviderSnapshot(configs);
        if (legacySnapshot != null) {
            savedConfigs.putIfAbsent(legacySnapshot.providerCode(), legacySnapshot);
        }
        return savedConfigs;
    }

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

    private AiModelCapabilities defaultCapabilities() {
        return AiModelCapabilities.builder()
                .supportsStream(true)
                .supportsToolCall(true)
                .supportsVision(false)
                .build();
    }

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
}

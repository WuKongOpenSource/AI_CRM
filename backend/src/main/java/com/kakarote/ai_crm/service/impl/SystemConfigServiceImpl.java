package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.ai.AiMode;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import com.kakarote.ai_crm.ai.provider.AiModelCapabilities;
import com.kakarote.ai_crm.ai.provider.AiProviderDescriptor;
import com.kakarote.ai_crm.ai.provider.AiProviderRegistry;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.AiConfigUpdateBO;
import com.kakarote.ai_crm.entity.BO.EnterpriseConfigUpdateBO;
import com.kakarote.ai_crm.entity.PO.SystemConfig;
import com.kakarote.ai_crm.entity.VO.AiConfigVO;
import com.kakarote.ai_crm.entity.VO.AiConnectionTestVO;
import com.kakarote.ai_crm.entity.VO.EnterpriseConfigVO;
import com.kakarote.ai_crm.mapper.SystemConfigMapper;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.ISystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现。
 */
@Slf4j
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig>
        implements ISystemConfigService {

    private static final String CACHE_KEY_PREFIX = "system:config:";
    private static final String AI_CONFIG_TYPE = "ai";
    private static final String ENTERPRISE_CONFIG_TYPE = "enterprise";
    private static final long CACHE_EXPIRE_MINUTES = 30;

    private static final String AI_MODE_KEY = "ai_mode";
    private static final String AI_PROVIDER_KEY = "ai_provider";
    private static final String AI_API_URL_KEY = "ai_api_url";
    private static final String AI_API_KEY_KEY = "ai_api_key";
    private static final String AI_MODEL_KEY = "ai_model";
    private static final String AI_TEMPERATURE_KEY = "ai_temperature";
    private static final String AI_MAX_TOKENS_KEY = "ai_max_tokens";
    private static final String AI_EXTRA_HEADERS_KEY = "ai_extra_headers";
    private static final String AI_PROVIDER_CONFIGS_KEY = "ai_provider_configs";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    @Lazy
    private DynamicChatClientProvider chatClientProvider;

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${spring.ai.openai.base-url:https://dashscope.aliyuncs.com/compatible-mode}")
    private String defaultApiUrl;

    @Value("${spring.ai.openai.chat.options.model:qwen3.6-plus}")
    private String defaultModel;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double defaultTemperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:2048}")
    private Integer defaultMaxTokens;

    private String buildCacheKey(String configKey) {
        return CACHE_KEY_PREFIX + configKey;
    }

    @Override
    public String getConfigValue(String configKey) {
        return getConfigValue(configKey, null);
    }

    @Override
    public String getConfigValue(String configKey, String defaultValue) {
        String cacheKey = buildCacheKey(configKey);
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            return cachedValue;
        }

        SystemConfig config = lambdaQuery()
                .eq(SystemConfig::getConfigKey, configKey)
                .one();

        if (config != null && StrUtil.isNotBlank(config.getConfigValue())) {
            redisTemplate.opsForValue().set(cacheKey, config.getConfigValue(),
                    CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            return config.getConfigValue();
        }

        return defaultValue;
    }

    @Override
    public Map<String, String> getConfigsByType(String configType) {
        List<SystemConfig> configs = lambdaQuery()
                .eq(SystemConfig::getConfigType, configType)
                .list();

        return configs.stream()
                .collect(Collectors.toMap(
                        SystemConfig::getConfigKey,
                        c -> c.getConfigValue() != null ? c.getConfigValue() : "",
                        (v1, v2) -> v2
                ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(String configKey, String configValue) {
        SystemConfig config = lambdaQuery()
                .eq(SystemConfig::getConfigKey, configKey)
                .one();

        if (config == null) {
            config = new SystemConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setConfigType(AI_CONFIG_TYPE);
            save(config);
        } else {
            config.setConfigValue(configValue);
            updateById(config);
        }

        redisTemplate.delete(buildCacheKey(configKey));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfigs(Map<String, String> configs) {
        configs.forEach(this::updateConfig);
    }

    @Override
    public AiConfigVO getAiConfig() {
        return buildAiConfig(false, false);
    }

    @Override
    public AiConfigVO getAiConfigDetail() {
        return buildAiConfig(true, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAiConfig(AiConfigUpdateBO updateBO) {
        String normalizedApiUrl = DynamicChatClientProvider.normalizeCompatibleBaseUrl(updateBO.getApiUrl());
        AiProviderDescriptor descriptor = AiProviderRegistry.resolve(updateBO.getProvider(), normalizedApiUrl);
        Map<String, String> existingConfigs = getConfigsByType(AI_CONFIG_TYPE);
        Map<String, StoredProviderConfig> providerConfigs = loadWritableProviderConfigs(existingConfigs);
        String normalizedApiKey = resolveApiKeyForSave(descriptor.getCode(), updateBO.getApiKey(), providerConfigs);
        updateBO.setApiKey(normalizedApiKey);
        AiModelCapabilities capabilities = validateAiConfig(descriptor, updateBO);

        String normalizedModel = updateBO.getModel().trim();
        Double resolvedTemperature = updateBO.getTemperature() != null ? updateBO.getTemperature() : defaultTemperature;
        Integer resolvedMaxTokens = updateBO.getMaxTokens() != null ? updateBO.getMaxTokens() : defaultMaxTokens;
        String normalizedExtraHeaders = StrUtil.nullToEmpty(updateBO.getExtraHeadersJson()).trim();

        providerConfigs.put(descriptor.getCode(), new StoredProviderConfig(
                descriptor.getCode(),
                normalizedApiUrl,
                normalizedApiKey,
                normalizedModel,
                resolvedTemperature,
                resolvedMaxTokens,
                normalizedExtraHeaders
        ));

        Map<String, String> configs = new HashMap<>();
        configs.put(AI_MODE_KEY, AiMode.CUSTOM.getCode());
        configs.put(AI_PROVIDER_KEY, descriptor.getCode());
        configs.put(AI_API_URL_KEY, normalizedApiUrl);
        configs.put(AI_API_KEY_KEY, normalizedApiKey);
        configs.put(AI_MODEL_KEY, normalizedModel);
        configs.put(AI_TEMPERATURE_KEY, String.valueOf(resolvedTemperature));
        configs.put(AI_MAX_TOKENS_KEY, String.valueOf(resolvedMaxTokens));
        configs.put(AI_EXTRA_HEADERS_KEY, normalizedExtraHeaders);
        configs.put(AI_PROVIDER_CONFIGS_KEY, serializeProviderConfigs(providerConfigs));

        updateConfigs(configs);
        chatClientProvider.refreshChatClient();

        log.info("AI 配置已更新: mode=custom, provider={}, model={}, supportsToolCall={}, supportsVision={}",
                descriptor.getCode(), updateBO.getModel(),
                capabilities.isSupportsToolCall(), capabilities.isSupportsVision());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateAiProvider(String provider) {
        Map<String, String> configs = getConfigsByType(AI_CONFIG_TYPE);
        SavedProviderConfigSnapshot targetConfig = resolveProviderToActivate(configs, provider);
        if (targetConfig == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "所选服务商尚未保存可用的 AI 配置");
        }

        updateConfigs(buildActivationConfigMap(targetConfig, true));
        chatClientProvider.refreshChatClient();
        log.info("AI 自定义服务商已激活: provider={}, model={}",
                targetConfig.providerCode(), targetConfig.model());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useCustomAiConfig() {
        Map<String, String> configs = getConfigsByType(AI_CONFIG_TYPE);
        SavedProviderConfigSnapshot targetConfig = resolveProviderToActivate(configs, configs.get(AI_PROVIDER_KEY));
        if (targetConfig == null) {
            targetConfig = resolveSelectedSavedProvider(configs, loadSavedProviderConfigs(configs));
        }
        if (targetConfig == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "当前系统还没有保存可用的自建 AI 配置");
        }
        updateConfigs(buildActivationConfigMap(targetConfig, true));
        chatClientProvider.refreshChatClient();
        log.info("AI 模式已切换为自定义模型: provider={}, model={}",
                targetConfig.providerCode(), targetConfig.model());
    }

    @Override
    public AiConnectionTestVO testAiConnection(AiConfigUpdateBO configBO) {
        AiConnectionTestVO result = new AiConnectionTestVO();
        long startTime = System.currentTimeMillis();
        String normalizedApiUrl = DynamicChatClientProvider.normalizeCompatibleBaseUrl(configBO.getApiUrl());
        AiProviderDescriptor descriptor = AiProviderRegistry.resolve(configBO.getProvider(), normalizedApiUrl);
        result.setProvider(descriptor.getCode());

        try {
            AiModelCapabilities capabilities = validateAiConfig(descriptor, configBO);

            ChatClient testClient = chatClientProvider.createTestChatClient(
                    descriptor.getCode(),
                    normalizedApiUrl,
                    configBO.getApiKey().trim(),
                    configBO.getModel().trim(),
                    configBO.getTemperature(),
                    configBO.getMaxTokens(),
                    configBO.getExtraHeadersJson(),
                    capabilities
            );

            String response = testClient.prompt()
                    .user("请只回复 OK")
                    .call()
                    .content();

            result.setSuccess(true);
            result.setMessage(response);
            result.setModel(configBO.getModel());
        } catch (BusinessException e) {
            result.setSuccess(false);
            result.setMessage(e.getMsg());
        } catch (Exception e) {
            log.error("AI 连接测试失败: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("连接失败: " + extractErrorMessage(e));
        }

        result.setResponseTime(System.currentTimeMillis() - startTime);
        return result;
    }

    @Override
    public void clearConfigCache() {
        Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("已清除 {} 个配置缓存", keys.size());
        }
    }

    @Override
    public EnterpriseConfigVO getEnterpriseConfig() {
        Map<String, String> configs = getConfigsByType(ENTERPRISE_CONFIG_TYPE);

        EnterpriseConfigVO vo = new EnterpriseConfigVO();
        vo.setName(configs.getOrDefault("enterprise_name", null));
        vo.setDescription(configs.getOrDefault("enterprise_description", null));

        String logo = configs.getOrDefault("enterprise_logo", null);
        vo.setLogo(logo);
        if (StrUtil.isNotBlank(logo)) {
            vo.setLogoUrl(fileStorageService.getUrl(logo));
        }

        SystemConfig latestConfig = lambdaQuery()
                .eq(SystemConfig::getConfigType, ENTERPRISE_CONFIG_TYPE)
                .orderByDesc(SystemConfig::getUpdateTime)
                .last("LIMIT 1")
                .one();
        if (latestConfig != null) {
            vo.setUpdateTime(latestConfig.getUpdateTime());
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEnterpriseConfig(EnterpriseConfigUpdateBO updateBO) {
        Map<String, String> configs = new HashMap<>();

        if (updateBO.getName() != null) {
            configs.put("enterprise_name", updateBO.getName());
        }
        if (updateBO.getLogo() != null) {
            configs.put("enterprise_logo", updateBO.getLogo());
        }
        if (updateBO.getDescription() != null) {
            configs.put("enterprise_description", updateBO.getDescription());
        }

        if (!configs.isEmpty()) {
            updateConfigsWithType(configs, ENTERPRISE_CONFIG_TYPE);
            log.info("企业信息配置已更新");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateConfigsWithType(Map<String, String> configs, String configType) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String configKey = entry.getKey();
            String configValue = entry.getValue();

            SystemConfig config = lambdaQuery()
                    .eq(SystemConfig::getConfigKey, configKey)
                    .one();

            if (config == null) {
                config = new SystemConfig();
                config.setConfigKey(configKey);
                config.setConfigValue(configValue);
                config.setConfigType(configType);
                save(config);
            } else {
                config.setConfigValue(configValue);
                updateById(config);
            }

            redisTemplate.delete(buildCacheKey(configKey));
        }
    }

    private AiConfigVO buildAiConfig(boolean includeSensitiveExtraHeaders, boolean detailView) {
        Map<String, String> configs = getConfigsByType(AI_CONFIG_TYPE);
        Map<String, SavedProviderConfigSnapshot> savedProviderConfigs = loadSavedProviderConfigs(configs);
        boolean customConfigSaved = !savedProviderConfigs.isEmpty();
        SavedProviderConfigSnapshot selectedSavedProvider = resolveSelectedSavedProvider(configs, savedProviderConfigs);
        EffectiveAiConfigSnapshot effectiveSnapshot = resolveEffectiveAiSnapshot(configs, selectedSavedProvider);
        DisplayAiConfigSnapshot displaySnapshot = detailView
                ? resolveDetailDisplaySnapshot(selectedSavedProvider, effectiveSnapshot)
                : DisplayAiConfigSnapshot.fromEffective(effectiveSnapshot);

        AiProviderDescriptor descriptor = AiProviderRegistry.resolve(displaySnapshot.providerCode(), displaySnapshot.apiUrl());
        AiModelCapabilities capabilities = descriptor.resolveCapabilities(displaySnapshot.model());

        AiConfigVO vo = new AiConfigVO();
        vo.setProvider(descriptor.getCode());
        vo.setProviderLabel(descriptor.getDisplayName());
        vo.setApiUrl(displaySnapshot.apiUrl());
        vo.setApiKey(maskApiKey(displaySnapshot.apiKey()));
        vo.setModel(displaySnapshot.model());
        vo.setTemperature(displaySnapshot.temperature());
        vo.setMaxTokens(displaySnapshot.maxTokens());
        vo.setExtraHeadersConfigured(StrUtil.isNotBlank(displaySnapshot.extraHeadersJson()));
        vo.setExtraHeadersJson(includeSensitiveExtraHeaders ? StrUtil.blankToDefault(displaySnapshot.extraHeadersJson(), "") : null);
        vo.setCapabilities(toCapabilitiesVO(capabilities));
        vo.setModelHint(descriptor.getModelHint());
        vo.setExtraHeadersHint(descriptor.getExtraHeadersHint());
        vo.setAvailableProviders(buildProviderOptions(
                savedProviderConfigs,
                detailView && includeSensitiveExtraHeaders,
                selectedSavedProvider != null ? selectedSavedProvider.providerCode() : null
        ));
        vo.setMode(effectiveSnapshot.mode().getCode());
        vo.setCustomConfigSaved(customConfigSaved);
        vo.setReady(isAiReady(effectiveSnapshot.apiKey()));
        vo.setUpdateTime(getLatestAiConfigUpdateTime());
        return vo;
    }

    private EffectiveAiConfigSnapshot resolveEffectiveAiSnapshot(Map<String, String> configs,
                                                                 SavedProviderConfigSnapshot selectedSavedProvider) {
        if (selectedSavedProvider != null) {
            return new EffectiveAiConfigSnapshot(
                    AiMode.CUSTOM,
                    selectedSavedProvider.providerCode(),
                    selectedSavedProvider.apiUrl(),
                    selectedSavedProvider.apiKey(),
                    selectedSavedProvider.model(),
                    selectedSavedProvider.temperature(),
                    selectedSavedProvider.maxTokens(),
                    selectedSavedProvider.extraHeadersJson()
            );
        }

        String normalizedApiUrl = DynamicChatClientProvider.normalizeCompatibleBaseUrl(
                StrUtil.blankToDefault(configs.get(AI_API_URL_KEY), defaultApiUrl)
        );
        String providerCode = AiProviderRegistry.resolve(configs.get(AI_PROVIDER_KEY), normalizedApiUrl).getCode();
        String resolvedModel = StrUtil.blankToDefault(configs.get(AI_MODEL_KEY), defaultModel);
        return new EffectiveAiConfigSnapshot(
                AiMode.CUSTOM,
                providerCode,
                normalizedApiUrl,
                "",
                resolvedModel,
                defaultTemperature,
                defaultMaxTokens,
                null
        );
    }

    private DisplayAiConfigSnapshot resolveDetailDisplaySnapshot(SavedProviderConfigSnapshot selectedSavedProvider,
                                                                 EffectiveAiConfigSnapshot effectiveSnapshot) {
        if (selectedSavedProvider != null) {
            return new DisplayAiConfigSnapshot(
                    selectedSavedProvider.providerCode(),
                    selectedSavedProvider.apiUrl(),
                    selectedSavedProvider.apiKey(),
                    selectedSavedProvider.model(),
                    selectedSavedProvider.temperature(),
                    selectedSavedProvider.maxTokens(),
                    selectedSavedProvider.extraHeadersJson()
            );
        }
        return DisplayAiConfigSnapshot.fromEffective(effectiveSnapshot);
    }

    private boolean isAiReady(String apiKey) {
        return StrUtil.isNotBlank(apiKey);
    }

    private List<AiConfigVO.ProviderOptionVO> buildProviderOptions(Map<String, SavedProviderConfigSnapshot> savedProviderConfigs,
                                                                  boolean includeSensitiveExtraHeaders,
                                                                  String activeProviderCode) {
        return AiProviderRegistry.list().stream()
                .map(descriptor -> {
                    AiConfigVO.ProviderOptionVO option = new AiConfigVO.ProviderOptionVO();
                    option.setValue(descriptor.getCode());
                    option.setLabel(descriptor.getDisplayName());
                    option.setDescription(descriptor.getDescription());
                    option.setBaseUrl(descriptor.getBaseUrl());
                    option.setModels(descriptor.getRecommendedModels());
                    option.setModelHint(descriptor.getModelHint());
                    option.setExtraHeadersHint(descriptor.getExtraHeadersHint());
                    AiModelCapabilities capabilities = descriptor.getDefaultCapabilities();
                    option.setSupportsStream(capabilities.isSupportsStream());
                    option.setSupportsToolCall(capabilities.isSupportsToolCall());
                    option.setSupportsVision(capabilities.isSupportsVision());
                    option.setSupportsAudioTranscription(capabilities.isSupportsAudioTranscription());

                    SavedProviderConfigSnapshot savedConfig = savedProviderConfigs.get(descriptor.getCode());
                    option.setConfigured(savedConfig != null);
                    option.setActive(savedConfig != null
                            && StrUtil.isNotBlank(activeProviderCode)
                            && descriptor.getCode().equalsIgnoreCase(activeProviderCode));
                    option.setApiKeyConfigured(savedConfig != null && StrUtil.isNotBlank(savedConfig.apiKey()));
                    option.setSavedApiUrl(savedConfig != null ? savedConfig.apiUrl() : null);
                    option.setSavedModel(savedConfig != null ? savedConfig.model() : null);
                    option.setSavedTemperature(savedConfig != null ? savedConfig.temperature() : null);
                    option.setSavedMaxTokens(savedConfig != null ? savedConfig.maxTokens() : null);
                    option.setSavedExtraHeadersConfigured(savedConfig != null && StrUtil.isNotBlank(savedConfig.extraHeadersJson()));
                    option.setSavedExtraHeadersJson(savedConfig != null && includeSensitiveExtraHeaders
                            ? StrUtil.blankToDefault(savedConfig.extraHeadersJson(), "")
                            : null);
                    return option;
                })
                .collect(Collectors.toList());
    }

    private AiConfigVO.CapabilitiesVO toCapabilitiesVO(AiModelCapabilities capabilities) {
        AiConfigVO.CapabilitiesVO vo = new AiConfigVO.CapabilitiesVO();
        vo.setSupportsStream(capabilities.isSupportsStream());
        vo.setSupportsToolCall(capabilities.isSupportsToolCall());
        vo.setSupportsVision(capabilities.isSupportsVision());
        vo.setSupportsAudioTranscription(capabilities.isSupportsAudioTranscription());
        return vo;
    }

    private AiModelCapabilities validateAiConfig(AiProviderDescriptor descriptor, AiConfigUpdateBO updateBO) {
        if (StrUtil.isBlank(updateBO.getApiUrl())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "API 地址不能为空");
        }
        if (StrUtil.isBlank(updateBO.getApiKey())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "API Key 不能为空");
        }
        if (StrUtil.isBlank(updateBO.getModel())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "模型名称不能为空");
        }

        validateExtraHeadersJson(updateBO.getExtraHeadersJson());

        AiModelCapabilities capabilities = descriptor.resolveCapabilities(updateBO.getModel());
        if (!capabilities.isSupportsToolCall()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                    "当前模型不支持工具调用，不适合作为 CRM 主对话模型，请改用支持 Tool Calling 的模型");
        }
        return capabilities;
    }

    private String resolveApiKeyForSave(String providerCode, String inputApiKey,
                                        Map<String, StoredProviderConfig> providerConfigs) {
        String normalizedApiKey = StrUtil.nullToEmpty(inputApiKey).trim();
        if (StrUtil.isNotBlank(normalizedApiKey)) {
            return normalizedApiKey;
        }

        StoredProviderConfig savedConfig = providerConfigs.get(providerCode);
        if (savedConfig == null) {
            return normalizedApiKey;
        }
        return StrUtil.nullToEmpty(savedConfig.apiKey()).trim();
    }

    private void validateExtraHeadersJson(String extraHeadersJson) {
        if (StrUtil.isBlank(extraHeadersJson)) {
            return;
        }

        try {
            Map<String, Object> headerMap = objectMapper.readValue(
                    extraHeadersJson,
                    new TypeReference<Map<String, Object>>() {
                    }
            );
            headerMap.forEach((key, value) -> {
                if (StrUtil.isBlank(key)) {
                    throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "额外请求头 JSON 中存在空键名");
                }
                if (value == null) {
                    throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                            String.format("额外请求头 %s 的值不能为空", key));
                }
            });
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                    "额外请求头 JSON 格式不正确，请传入对象格式，例如 {\"appid\":\"your-app-id\"}");
        }
    }

    private SavedProviderConfigSnapshot resolveProviderToActivate(Map<String, String> configs, String provider) {
        Map<String, SavedProviderConfigSnapshot> savedProviderConfigs = loadSavedProviderConfigs(configs);
        if (savedProviderConfigs.isEmpty()) {
            return null;
        }

        if (StrUtil.isNotBlank(provider)) {
            return savedProviderConfigs.get(provider.trim().toLowerCase());
        }

        return resolveSelectedSavedProvider(configs, savedProviderConfigs);
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

    private Map<String, SavedProviderConfigSnapshot> loadSavedProviderConfigs(Map<String, String> configs) {
        Map<String, SavedProviderConfigSnapshot> savedConfigs = new HashMap<>();
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

    private Map<String, StoredProviderConfig> loadWritableProviderConfigs(Map<String, String> configs) {
        Map<String, StoredProviderConfig> storedConfigs = new HashMap<>(parseStoredProviderConfigs(configs.get(AI_PROVIDER_CONFIGS_KEY)));
        SavedProviderConfigSnapshot legacySnapshot = buildLegacyProviderSnapshot(configs);
        if (legacySnapshot != null) {
            storedConfigs.putIfAbsent(legacySnapshot.providerCode(), toStoredProviderConfig(legacySnapshot));
        }
        return storedConfigs;
    }

    private Map<String, StoredProviderConfig> parseStoredProviderConfigs(String providerConfigsJson) {
        if (StrUtil.isBlank(providerConfigsJson)) {
            return new HashMap<>();
        }

        try {
            Map<String, StoredProviderConfig> storedConfigs = objectMapper.readValue(
                    providerConfigsJson,
                    new TypeReference<Map<String, StoredProviderConfig>>() {
                    }
            );
            return storedConfigs != null ? storedConfigs : new HashMap<>();
        } catch (Exception e) {
            log.warn("解析多服务商 AI 配置失败，将回退到旧版单配置: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private String serializeProviderConfigs(Map<String, StoredProviderConfig> providerConfigs) {
        try {
            return objectMapper.writeValueAsString(providerConfigs);
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "序列化 AI 服务商配置失败");
        }
    }

    private SavedProviderConfigSnapshot buildLegacyProviderSnapshot(Map<String, String> configs) {
        String apiUrl = DynamicChatClientProvider.normalizeCompatibleBaseUrl(configs.get(AI_API_URL_KEY));
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

        String apiUrl = DynamicChatClientProvider.normalizeCompatibleBaseUrl(storedConfig.apiUrl());
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

    private StoredProviderConfig toStoredProviderConfig(SavedProviderConfigSnapshot snapshot) {
        return new StoredProviderConfig(
                snapshot.providerCode(),
                snapshot.apiUrl(),
                snapshot.apiKey(),
                snapshot.model(),
                snapshot.temperature(),
                snapshot.maxTokens(),
                StrUtil.nullToEmpty(snapshot.extraHeadersJson())
        );
    }

    private Map<String, String> buildActivationConfigMap(SavedProviderConfigSnapshot snapshot, boolean includeMode) {
        Map<String, String> configs = new HashMap<>();
        if (includeMode) {
            configs.put(AI_MODE_KEY, AiMode.CUSTOM.getCode());
        }
        configs.put(AI_PROVIDER_KEY, snapshot.providerCode());
        configs.put(AI_API_URL_KEY, snapshot.apiUrl());
        configs.put(AI_API_KEY_KEY, snapshot.apiKey());
        configs.put(AI_MODEL_KEY, snapshot.model());
        configs.put(AI_TEMPERATURE_KEY, String.valueOf(snapshot.temperature()));
        configs.put(AI_MAX_TOKENS_KEY, String.valueOf(snapshot.maxTokens()));
        configs.put(AI_EXTRA_HEADERS_KEY, StrUtil.nullToEmpty(snapshot.extraHeadersJson()));
        return configs;
    }

    private Date getLatestAiConfigUpdateTime() {
        SystemConfig anyConfig = lambdaQuery()
                .eq(SystemConfig::getConfigType, AI_CONFIG_TYPE)
                .orderByDesc(SystemConfig::getUpdateTime)
                .last("LIMIT 1")
                .one();
        return anyConfig != null ? anyConfig.getUpdateTime() : null;
    }

    private String maskApiKey(String apiKey) {
        if (StrUtil.isBlank(apiKey)) {
            return "";
        }
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 3) + "****" + apiKey.substring(apiKey.length() - 4);
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

    private String extractErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return "未知错误";
        }
        if (message.contains("401")) {
            return "API Key 无效或已过期";
        }
        if (message.contains("403")) {
            return "API Key 权限不足";
        }
        if (message.contains("429")) {
            return "请求频率超限，请稍后重试";
        }
        if (message.contains("Connection refused") || message.contains("UnknownHost")) {
            return "无法连接到 API 服务，请检查地址";
        }
        if (message.contains("timeout")) {
            return "连接超时";
        }
        if (message.length() > 100) {
            return message.substring(0, 100) + "...";
        }
        return message;
    }

    private record EffectiveAiConfigSnapshot(
            AiMode mode,
            String providerCode,
            String apiUrl,
            String apiKey,
            String model,
            Double temperature,
            Integer maxTokens,
            String extraHeadersJson
    ) {
    }

    private record DisplayAiConfigSnapshot(
            String providerCode,
            String apiUrl,
            String apiKey,
            String model,
            Double temperature,
            Integer maxTokens,
            String extraHeadersJson
    ) {
        private static DisplayAiConfigSnapshot fromEffective(EffectiveAiConfigSnapshot snapshot) {
            return new DisplayAiConfigSnapshot(
                    snapshot.providerCode(),
                    snapshot.apiUrl(),
                    snapshot.apiKey(),
                    snapshot.model(),
                    snapshot.temperature(),
                    snapshot.maxTokens(),
                    snapshot.extraHeadersJson()
            );
        }
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

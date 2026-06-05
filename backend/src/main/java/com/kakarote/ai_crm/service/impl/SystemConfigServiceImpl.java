package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.ai.AiMode;
import com.kakarote.ai_crm.ai.AiModelSource;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import com.kakarote.ai_crm.ai.provider.AiModelCapabilities;
import com.kakarote.ai_crm.ai.provider.AiProviderDescriptor;
import com.kakarote.ai_crm.ai.provider.AiProviderRegistry;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import com.kakarote.ai_crm.entity.BO.AiConfigUpdateBO;
import com.kakarote.ai_crm.entity.BO.EnterpriseConfigUpdateBO;
import com.kakarote.ai_crm.entity.PO.CrmTenant;
import com.kakarote.ai_crm.entity.PO.SystemConfig;
import com.kakarote.ai_crm.entity.VO.AiBillingConfigVO;
import com.kakarote.ai_crm.entity.VO.AiConfigVO;
import com.kakarote.ai_crm.entity.VO.AiConnectionTestVO;
import com.kakarote.ai_crm.entity.VO.EnterpriseConfigVO;
import com.kakarote.ai_crm.mapper.SystemConfigMapper;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.ICrmTenantService;
import com.kakarote.ai_crm.service.ISystemConfigService;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private static final String ENTERPRISE_NAME_KEY = "enterprise_name";
    private static final String ENTERPRISE_LOGO_KEY = "enterprise_logo";
    private static final String ENTERPRISE_DESCRIPTION_KEY = "enterprise_description";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private DynamicChatClientProvider chatClientProvider;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ICrmTenantService tenantService;

    @Autowired
    private AiQuotaService aiQuotaService;

    @Autowired
    private AiBillingConfigService aiBillingConfigService;

    @Value("${spring.ai.openai.base-url:https://dashscope.aliyuncs.com/compatible-mode}")
    private String defaultApiUrl;

    @Value("${spring.ai.openai.api-key:${DASHSCOPE_API_KEY:${OPENAI_API_KEY:}}}")
    private String defaultApiKey;

    @Value("${spring.ai.openai.chat.options.model:qwen3.6-plus}")
    private String defaultModel;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double defaultTemperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:2048}")
    private Integer defaultMaxTokens;

    @Value("${weknora.init-models.chat.base-url:}")
    private String giftApiUrl;

    @Value("${weknora.init-models.chat.api-key:}")
    private String giftApiKey;

    @Value("${weknora.init-models.chat.name:}")
    private String giftModel;

    /**
     * 构建缓存键。
     */
    private String buildCacheKey(String configKey) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return CACHE_KEY_PREFIX + tenantId + ":" + configKey;
        }
        return CACHE_KEY_PREFIX + configKey;
    }

    /**
     * 获取配置值。
     */
    @Override
    public String getConfigValue(String configKey) {
        return getConfigValue(configKey, null);
    }

    /**
     * 获取配置值。
     */
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

    /**
     * 获取配置按类型。
     */
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

    /**
     * 更新配置。
     */
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

    /**
     * 更新配置。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfigs(Map<String, String> configs) {
        configs.forEach(this::updateConfig);
    }

    /**
     * 获取AI 配置。
     */
    @Override
    public AiConfigVO getAiConfig() {
        return buildAiConfig(false, false);
    }

    /**
     * 获取AI 配置详情。
     */
    @Override
    public AiConfigVO getAiConfigDetail() {
        return buildAiConfig(true, true);
    }

    /**
     * 更新AI 配置。
     */
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

        log.info("AI 配置已更新: tenantId={}, mode=custom, provider={}, model={}, supportsToolCall={}, supportsVision={}",
                UserUtil.getTenantId(), descriptor.getCode(), updateBO.getModel(),
                capabilities.isSupportsToolCall(), capabilities.isSupportsVision());
    }

    /**
     * 激活AI 服务商。
     */
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
        log.info("AI 自定义服务商已激活: tenantId={}, provider={}, model={}",
                UserUtil.getTenantId(), targetConfig.providerCode(), targetConfig.model());
    }

    /**
     * 切换使用赠送AI 配置。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useGiftAiConfig() {
        updateConfig(AI_MODE_KEY, AiMode.GIFT.getCode());
        chatClientProvider.refreshChatClient();
        log.info("AI 模式已切换为赠送额度: tenantId={}", UserUtil.getTenantId());
    }

    /**
     * 切换使用自定义AI 配置。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useCustomAiConfig() {
        Map<String, String> configs = getConfigsByType(AI_CONFIG_TYPE);
        SavedProviderConfigSnapshot targetConfig = resolveProviderToActivate(configs, configs.get(AI_PROVIDER_KEY));
        if (targetConfig == null) {
            targetConfig = resolveSelectedSavedProvider(configs, loadSavedProviderConfigs(configs));
        }
        if (targetConfig == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "当前租户还没有保存可用的自定义 AI 配置");
        }
        updateConfigs(buildActivationConfigMap(targetConfig, true));
        chatClientProvider.refreshChatClient();
        log.info("AI 模式已切换为自定义模型: tenantId={}, provider={}, model={}",
                UserUtil.getTenantId(), targetConfig.providerCode(), targetConfig.model());
    }

    @Override
    public AiBillingConfigVO getAiBillingConfig() {
        return aiBillingConfigService.getConfig();
    }

    /**
     * 处理testAiConnection方法逻辑。
     */
    @Override
    public AiConnectionTestVO testAiConnection(AiConfigUpdateBO configBO) {
        AiConnectionTestVO result = new AiConnectionTestVO();
        long startTime = System.currentTimeMillis();
        String normalizedApiUrl = DynamicChatClientProvider.normalizeCompatibleBaseUrl(configBO.getApiUrl());
        AiProviderDescriptor descriptor = AiProviderRegistry.resolve(configBO.getProvider(), normalizedApiUrl);
        result.setProvider(descriptor.getCode());

        try {
            String quotaFailureMessage = aiQuotaService.resolveQuotaFailureMessage(
                UserUtil.getTenantId(), "system_ai_test", 32, null, AiModelSource.CUSTOM);
            if (quotaFailureMessage != null) {
                result.setSuccess(false);
                result.setMessage(quotaFailureMessage);
                result.setResponseTime(System.currentTimeMillis() - startTime);
                return result;
            }

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

            var chatResponse = testClient.prompt()
                    .user("请只回复 OK")
                    .call()
                    .chatResponse();

            String response = chatResponse.getResult().getOutput().getText();
            aiQuotaService.consumeResolvedTokens(
                UserUtil.getTenantId(),
                "system_ai_test",
                aiQuotaService.resolveTokenUsage(chatResponse, null, null, "请只回复 OK", response),
                null,
                AiModelSource.CUSTOM,
                descriptor.getCode(),
                configBO.getModel(),
                null,
                null
            );

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

    /**
     * 清理配置缓存。
     */
    @Override
    public void clearConfigCache() {
        Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("已清除 {} 个配置缓存", keys.size());
        }
    }

    /**
     * 获取企业配置。
     */
    @Override
    public EnterpriseConfigVO getEnterpriseConfig() {
        Map<String, String> configs = getConfigsByType(ENTERPRISE_CONFIG_TYPE);

        EnterpriseConfigVO vo = new EnterpriseConfigVO();
        vo.setName(configs.containsKey(ENTERPRISE_NAME_KEY)
                ? configs.get(ENTERPRISE_NAME_KEY)
                : resolveCurrentTenantName());
        vo.setDescription(configs.getOrDefault(ENTERPRISE_DESCRIPTION_KEY, null));

        String logo = configs.getOrDefault(ENTERPRISE_LOGO_KEY, null);
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

    /**
     * 更新企业配置。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEnterpriseConfig(EnterpriseConfigUpdateBO updateBO) {
        Map<String, String> configs = new HashMap<>();

        if (updateBO.getName() != null) {
            configs.put(ENTERPRISE_NAME_KEY, updateBO.getName());
        }
        if (updateBO.getLogo() != null) {
            configs.put(ENTERPRISE_LOGO_KEY, updateBO.getLogo());
        }
        if (updateBO.getDescription() != null) {
            configs.put(ENTERPRISE_DESCRIPTION_KEY, updateBO.getDescription());
        }

        if (!configs.isEmpty()) {
            updateConfigsWithType(configs, ENTERPRISE_CONFIG_TYPE);
            log.info("企业信息配置已更新");
        }
    }

    /**
     * 解析当前租户名称。
     */
    private String resolveCurrentTenantName() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return null;
        }

        CrmTenant tenant = tenantService.getById(tenantId);
        return tenant != null ? StrUtil.nullToEmpty(tenant.getTenantName()).trim() : null;
    }

    /**
     * 更新配置包含类型。
     */
    @Override
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

    /**
     * 构建AI 配置。
     */
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

        Long tenantId = UserUtil.getTenantId();
        long giftCreditTotal = tenantService.getGiftCreditTotal(tenantId);
        long giftCreditUsed = tenantService.getGiftCreditUsed(tenantId);
        long giftCreditRemaining = tenantService.getGiftCreditRemaining(tenantId);
        long purchasedCreditTotal = tenantService.getPurchasedCreditTotal(tenantId);
        long purchasedCreditUsed = tenantService.getPurchasedCreditUsed(tenantId);
        long purchasedCreditRemaining = tenantService.getPurchasedCreditRemaining(tenantId);
        long creditTotal = giftCreditTotal + purchasedCreditTotal;
        long creditUsed = giftCreditUsed + purchasedCreditUsed;
        long creditRemaining = giftCreditRemaining + purchasedCreditRemaining;

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
                detailView ? savedProviderConfigs : Map.of(),
                detailView && includeSensitiveExtraHeaders,
                selectedSavedProvider != null ? selectedSavedProvider.providerCode() : null
        ));
        vo.setMode(effectiveSnapshot.mode().getCode());
        vo.setCustomConfigSaved(customConfigSaved);
        vo.setReady(isAiReady(effectiveSnapshot.mode(), effectiveSnapshot.apiKey(), creditRemaining));
        vo.setGiftCreditTotal(giftCreditTotal);
        vo.setGiftCreditUsed(giftCreditUsed);
        vo.setGiftCreditRemaining(giftCreditRemaining);
        vo.setGiftCreditAvailable(giftCreditRemaining > 0);
        vo.setPurchasedCreditTotal(purchasedCreditTotal);
        vo.setPurchasedCreditUsed(purchasedCreditUsed);
        vo.setPurchasedCreditRemaining(purchasedCreditRemaining);
        vo.setCreditTotal(creditTotal);
        vo.setCreditUsed(creditUsed);
        vo.setCreditRemaining(creditRemaining);
        vo.setCreditAvailable(creditRemaining > 0);
        vo.setUpdateTime(getLatestAiConfigUpdateTime());
        return vo;
    }

    /**
     * 解析有效AI快照。
     */
    private EffectiveAiConfigSnapshot resolveEffectiveAiSnapshot(Map<String, String> configs,
                                                                 SavedProviderConfigSnapshot selectedSavedProvider) {
        AiMode requestedMode = AiMode.resolve(configs.get(AI_MODE_KEY));
        if (requestedMode == AiMode.CUSTOM && selectedSavedProvider != null) {
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

        String normalizedDefaultApiUrl = DynamicChatClientProvider.normalizeCompatibleBaseUrl(
                StrUtil.blankToDefault(giftApiUrl, defaultApiUrl)
        );
        String providerCode = AiProviderRegistry.resolve(null, normalizedDefaultApiUrl).getCode();
        String resolvedGiftApiKey = StrUtil.blankToDefault(giftApiKey, defaultApiKey);
        String resolvedGiftModel = StrUtil.blankToDefault(giftModel, defaultModel);
        return new EffectiveAiConfigSnapshot(
                AiMode.GIFT,
                providerCode,
                normalizedDefaultApiUrl,
                resolvedGiftApiKey,
                resolvedGiftModel,
                defaultTemperature,
                defaultMaxTokens,
                null
        );
    }

    /**
     * 解析详情Display快照。
     */
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

    /**
     * 判断是否AI就绪。
     */
    private boolean isAiReady(AiMode mode, String apiKey, long creditRemaining) {
        return StrUtil.isNotBlank(apiKey) && creditRemaining > 0;
    }

    /**
     * 构建服务商选项。
     */
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

    /**
     * 转换为能力VO。
     */
    private AiConfigVO.CapabilitiesVO toCapabilitiesVO(AiModelCapabilities capabilities) {
        AiConfigVO.CapabilitiesVO vo = new AiConfigVO.CapabilitiesVO();
        vo.setSupportsStream(capabilities.isSupportsStream());
        vo.setSupportsToolCall(capabilities.isSupportsToolCall());
        vo.setSupportsVision(capabilities.isSupportsVision());
        vo.setSupportsAudioTranscription(capabilities.isSupportsAudioTranscription());
        return vo;
    }

    /**
     * 校验AI 配置。
     */
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

    /**
     * 解析API Key用于Save。
     */
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

    /**
     * 校验Extra请求头JSON。
     */
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

    /**
     * 判断是否存在已保存自定义AI 配置。
     */
    private boolean hasSavedCustomAiConfig(Map<String, String> configs) {
        return !loadSavedProviderConfigs(configs).isEmpty();
    }

    /**
     * 解析服务商TOActivate。
     */
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
     * 加载已保存服务商配置。
     */
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

    /**
     * 加载可写服务商配置。
     */
    private Map<String, StoredProviderConfig> loadWritableProviderConfigs(Map<String, String> configs) {
        Map<String, StoredProviderConfig> storedConfigs = new HashMap<>(parseStoredProviderConfigs(configs.get(AI_PROVIDER_CONFIGS_KEY)));
        SavedProviderConfigSnapshot legacySnapshot = buildLegacyProviderSnapshot(configs);
        if (legacySnapshot != null) {
            storedConfigs.putIfAbsent(legacySnapshot.providerCode(), toStoredProviderConfig(legacySnapshot));
        }
        return storedConfigs;
    }

    /**
     * 解析已存储服务商配置。
     */
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

    /**
     * 处理serializeProviderConfigs方法逻辑。
     */
    private String serializeProviderConfigs(Map<String, StoredProviderConfig> providerConfigs) {
        try {
            return objectMapper.writeValueAsString(providerConfigs);
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "序列化 AI 服务商配置失败");
        }
    }

    /**
     * 构建旧版服务商快照。
     */
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

    /**
     * 转换为已保存服务商配置快照。
     */
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

    /**
     * 转换为已存储服务商配置。
     */
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

    /**
     * 构建Activation配置MAP。
     */
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

    /**
     * 获取最新AI 配置Update时间。
     */
    private Date getLatestAiConfigUpdateTime() {
        SystemConfig anyConfig = lambdaQuery()
                .eq(SystemConfig::getConfigType, AI_CONFIG_TYPE)
                .orderByDesc(SystemConfig::getUpdateTime)
                .last("LIMIT 1")
                .one();
        return anyConfig != null ? anyConfig.getUpdateTime() : null;
    }

    /**
     * 脱敏API Key。
     */
    private String maskApiKey(String apiKey) {
        if (StrUtil.isBlank(apiKey)) {
            return "";
        }
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 3) + "****" + apiKey.substring(apiKey.length() - 4);
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

    /**
     * 处理extractErrorMessage方法逻辑。
     */
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
        /**
         * 处理from有效方法逻辑。
         */
        private static DisplayAiConfigSnapshot fromEffective(EffectiveAiConfigSnapshot snapshot) {
            String apiKey = snapshot.mode() == AiMode.CUSTOM ? snapshot.apiKey() : "";
            String extraHeadersJson = snapshot.mode() == AiMode.CUSTOM ? snapshot.extraHeadersJson() : null;
            return new DisplayAiConfigSnapshot(
                    snapshot.providerCode(),
                    snapshot.apiUrl(),
                    apiKey,
                    snapshot.model(),
                    snapshot.temperature(),
                    snapshot.maxTokens(),
                    extraHeadersJson
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

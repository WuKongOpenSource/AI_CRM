package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import com.kakarote.ai_crm.entity.BO.AiConfigUpdateBO;
import com.kakarote.ai_crm.entity.BO.EnterpriseConfigUpdateBO;
import com.kakarote.ai_crm.entity.BO.WeKnoraConfigUpdateBO;
import com.kakarote.ai_crm.entity.BO.WeKnoraModelTestBO;
import com.kakarote.ai_crm.entity.BO.WeKnoraModelSyncBO;
import com.kakarote.ai_crm.entity.PO.SystemConfig;
import com.kakarote.ai_crm.entity.VO.AiConfigVO;
import com.kakarote.ai_crm.entity.VO.AiConnectionTestVO;
import com.kakarote.ai_crm.entity.VO.EnterpriseConfigVO;
import com.kakarote.ai_crm.entity.VO.WeKnoraConfigVO;
import com.kakarote.ai_crm.entity.VO.WeKnoraConnectionTestVO;
import com.kakarote.ai_crm.entity.VO.WeKnoraModelTestVO;
import com.kakarote.ai_crm.entity.VO.WeKnoraModelSyncVO;
import com.kakarote.ai_crm.mapper.SystemConfigMapper;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.ISystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现
 */
@Slf4j
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig>
        implements ISystemConfigService {

    private static final String CACHE_KEY_PREFIX = "system:config:";
    private static final String AI_CONFIG_TYPE = "ai";
    private static final String WEKNORA_CONFIG_TYPE = "weknora";
    private static final String ENTERPRISE_CONFIG_TYPE = "enterprise";
    private static final long CACHE_EXPIRE_MINUTES = 30;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private DynamicChatClientProvider chatClientProvider;

    @Autowired
    private FileStorageService fileStorageService;

    // 从 application.yml 读取默认值，确保与 DynamicChatClientProvider 一致
    @Value("${spring.ai.openai.base-url:https://dashscope.aliyuncs.com/compatible-mode/}")
    private String defaultApiUrl;

    @Value("${spring.ai.openai.chat.options.model:qwen-max}")
    private String defaultModel;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double defaultTemperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:2048}")
    private Integer defaultMaxTokens;

    // WeKnora 默认配置
    @Value("${weknora.enabled:false}")
    private boolean defaultWeKnoraEnabled;

    @Value("${weknora.base-url:http://localhost:8080/api/v1}")
    private String defaultWeKnoraBaseUrl;

    @Value("${weknora.api-key:}")
    private String defaultWeKnoraApiKey;

    @Value("${weknora.init.username:admin}")
    private String weKnoraInitUsername;

    @Value("${weknora.init.email:}")
    private String weKnoraInitEmail;

    @Value("${weknora.init.password:}")
    private String weKnoraInitPassword;

    @Value("${weknora.knowledge-base-id:}")
    private String defaultWeKnoraKnowledgeBaseId;

    @Value("${weknora.search.match-count:5}")
    private Integer defaultWeKnoraMatchCount;

    @Value("${weknora.search.vector-threshold:0.5}")
    private Double defaultWeKnoraVectorThreshold;

    @Value("${weknora.search.auto-rag-enabled:true}")
    private boolean defaultWeKnoraAutoRagEnabled;

    @Override
    public String getConfigValue(String configKey) {
        return getConfigValue(configKey, null);
    }

    @Override
    public String getConfigValue(String configKey, String defaultValue) {
        // 1. 先从 Redis 缓存查询
        String cacheKey = CACHE_KEY_PREFIX + configKey;
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            return cachedValue;
        }

        // 2. 从数据库查询
        SystemConfig config = lambdaQuery()
                .eq(SystemConfig::getConfigKey, configKey)
                .one();

        if (config != null && StrUtil.isNotBlank(config.getConfigValue())) {
            // 缓存到 Redis
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
            // 新增配置
            config = new SystemConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setConfigType(AI_CONFIG_TYPE);
            save(config);
        } else {
            // 更新配置
            config.setConfigValue(configValue);
            updateById(config);
        }

        // 清除缓存
        redisTemplate.delete(CACHE_KEY_PREFIX + configKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfigs(Map<String, String> configs) {
        configs.forEach(this::updateConfig);
    }

    @Override
    public AiConfigVO getAiConfig() {
        Map<String, String> configs = getConfigsByType(AI_CONFIG_TYPE);

        AiConfigVO vo = new AiConfigVO();

        // 如果 ai_provider 存在，说明用户已通过设置页面保存过配置，使用 DB 值
        // 如果不存在，说明从未通过设置页面配置，全部使用 application.yml 默认值
        boolean hasExplicitConfig = configs.containsKey("ai_provider");

        if (hasExplicitConfig) {
            vo.setProvider(configs.get("ai_provider"));
            vo.setApiUrl(configs.getOrDefault("ai_api_url", defaultApiUrl));
            vo.setApiKey(maskApiKey(configs.get("ai_api_key")));
            vo.setModel(configs.getOrDefault("ai_model", defaultModel));
            vo.setTemperature(parseDouble(configs.get("ai_temperature"), defaultTemperature));
            vo.setMaxTokens(parseInt(configs.get("ai_max_tokens"), defaultMaxTokens));
        } else {
            // 未通过设置页面配置过，返回 application.yml 默认值（通义千问）
            vo.setProvider("dashscope");
            vo.setApiUrl(defaultApiUrl);
            vo.setApiKey(maskApiKey(configs.get("ai_api_key")));
            vo.setModel(defaultModel);
            vo.setTemperature(defaultTemperature);
            vo.setMaxTokens(defaultMaxTokens);
        }

        // 获取最后更新时间
        SystemConfig anyConfig = lambdaQuery()
                .eq(SystemConfig::getConfigType, AI_CONFIG_TYPE)
                .orderByDesc(SystemConfig::getUpdateTime)
                .last("LIMIT 1")
                .one();
        if (anyConfig != null) {
            vo.setUpdateTime(anyConfig.getUpdateTime());
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAiConfig(AiConfigUpdateBO updateBO) {
        Map<String, String> configs = new HashMap<>();

        if (StrUtil.isNotBlank(updateBO.getProvider())) {
            configs.put("ai_provider", updateBO.getProvider());
        }
        if (StrUtil.isNotBlank(updateBO.getApiUrl())) {
            configs.put("ai_api_url", updateBO.getApiUrl());
        }
        // 前端会回显脱敏后的密钥；保存脱敏值会覆盖真实密钥，所以这里仅保存用户新输入的明文密钥。
        if (StrUtil.isNotBlank(updateBO.getApiKey()) && !isMaskedSecret(updateBO.getApiKey())) {
            configs.put("ai_api_key", updateBO.getApiKey());
        }
        if (StrUtil.isNotBlank(updateBO.getModel())) {
            configs.put("ai_model", updateBO.getModel());
        }
        if (updateBO.getTemperature() != null) {
            configs.put("ai_temperature", String.valueOf(updateBO.getTemperature()));
        }
        if (updateBO.getMaxTokens() != null) {
            configs.put("ai_max_tokens", String.valueOf(updateBO.getMaxTokens()));
        }

        updateConfigs(configs);

        // 重要：通知 DynamicChatClientProvider 刷新 ChatClient
        chatClientProvider.refreshChatClient();

        log.info("AI 配置已更新，ChatClient 已刷新");
    }

    @Override
    public AiConnectionTestVO testAiConnection(AiConfigUpdateBO configBO) {
        AiConnectionTestVO result = new AiConnectionTestVO();
        long startTime = System.currentTimeMillis();

        try {
            // 创建临时 ChatClient 进行测试
            // 如果前端传入脱敏密钥，则用数据库中保存的真实密钥完成测试。
            ChatClient testClient = chatClientProvider.createTestChatClient(
                    configBO.getApiUrl(),
                    resolveSecret(configBO.getApiKey(), getConfigsByType(AI_CONFIG_TYPE).get("ai_api_key"), "AI API Key"),
                    configBO.getModel(),
                    configBO.getTemperature(),
                    configBO.getMaxTokens()
            );

            // 发送简单测试消息
            String response = testClient.prompt()
                    .user("请回复 OK")
                    .call()
                    .content();

            result.setSuccess(true);
            result.setMessage(response);
            result.setModel(configBO.getModel());

        } catch (Exception e) {
            log.error("AI 连接测试失败: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("连接失败: " + extractErrorMessage(e));
        }

        result.setResponseTime(System.currentTimeMillis() - startTime);
        return result;
    }

    @Override
    public WeKnoraModelTestVO testWeKnoraModel(WeKnoraModelTestBO testBO) {
        WeKnoraModelTestVO result = new WeKnoraModelTestVO();
        long startTime = System.currentTimeMillis();

        try {
            Map<String, String> currentConfigs = getConfigsByType(WEKNORA_CONFIG_TYPE);
            String modelType = required(testBO.getModelType(), "model type").toLowerCase();

            if ("embedding".equals(modelType)) {
                String modelName = required(firstNotBlank(testBO.getModelName(), currentConfigs.get("weknora_embedding_model_name")), "Embedding model name");
                String baseUrl = normalizeProviderBaseUrl(firstNotBlank(testBO.getBaseUrl(), currentConfigs.get("weknora_embedding_base_url")));
                String apiKey = resolveSecret(testBO.getApiKey(), currentConfigs.get("weknora_embedding_api_key"), "Embedding API Key");
                testEmbeddingModel(result, baseUrl, apiKey, modelName);
            } else if ("llm".equals(modelType) || "knowledgeqa".equals(modelType)) {
                String modelName = required(firstNotBlank(testBO.getModelName(), currentConfigs.get("weknora_llm_model_name")), "LLM model name");
                String baseUrl = normalizeProviderBaseUrl(firstNotBlank(testBO.getBaseUrl(), currentConfigs.get("weknora_llm_base_url")));
                String apiKey = resolveSecret(testBO.getApiKey(), currentConfigs.get("weknora_llm_api_key"), "LLM API Key");
                testLlmModel(result, baseUrl, apiKey, modelName);
            } else {
                throw new IllegalArgumentException("Unsupported model type: " + testBO.getModelType());
            }
        } catch (Exception e) {
            log.error("WeKnora model connection test failed: {}", e.getMessage(), e);
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

    // ==================== WeKnora 配置相关方法 ====================

    @Override
    public WeKnoraConfigVO getWeKnoraConfig() {
        Map<String, String> configs = getConfigsByType(WEKNORA_CONFIG_TYPE);

        WeKnoraConfigVO vo = new WeKnoraConfigVO();

        // 检查是否有显式配置
        boolean hasExplicitConfig = configs.containsKey("weknora_enabled");

        if (hasExplicitConfig) {
            vo.setEnabled(parseBoolean(configs.get("weknora_enabled"), defaultWeKnoraEnabled));
            vo.setBaseUrl(getConfigValueOrDefault(configs, "weknora_base_url", defaultWeKnoraBaseUrl));
            vo.setApiKey(maskApiKey(configs.get("weknora_api_key")));
            vo.setKnowledgeBaseId(getConfigValueOrDefault(configs, "weknora_knowledge_base_id", defaultWeKnoraKnowledgeBaseId));
            vo.setKnowledgeBaseName(getConfigValueOrDefault(configs, "weknora_knowledge_base_name", "CRM Default Knowledge Base"));
            vo.setLlmProvider(getConfigValueOrDefault(configs, "weknora_llm_provider", "aliyun"));
            vo.setLlmModelName(configs.get("weknora_llm_model_name"));
            vo.setLlmBaseUrl(configs.get("weknora_llm_base_url"));
            vo.setLlmApiKey(maskApiKey(configs.get("weknora_llm_api_key")));
            vo.setLlmModelId(configs.get("weknora_llm_model_id"));
            vo.setEmbeddingProvider(getConfigValueOrDefault(configs, "weknora_embedding_provider", "aliyun"));
            vo.setEmbeddingModelName(configs.get("weknora_embedding_model_name"));
            vo.setEmbeddingBaseUrl(configs.get("weknora_embedding_base_url"));
            vo.setEmbeddingApiKey(maskApiKey(configs.get("weknora_embedding_api_key")));
            vo.setEmbeddingDimension(parseInt(configs.get("weknora_embedding_dimension"), 1024));
            vo.setEmbeddingModelId(configs.get("weknora_embedding_model_id"));
            vo.setMatchCount(parseInt(configs.get("weknora_match_count"), defaultWeKnoraMatchCount));
            vo.setVectorThreshold(parseDouble(configs.get("weknora_vector_threshold"), defaultWeKnoraVectorThreshold));
            vo.setAutoRagEnabled(parseBoolean(configs.get("weknora_auto_rag_enabled"), defaultWeKnoraAutoRagEnabled));
        } else {
            // 返回 application.yml 默认值
            vo.setEnabled(defaultWeKnoraEnabled);
            vo.setBaseUrl(defaultWeKnoraBaseUrl);
            vo.setApiKey(maskApiKey(defaultWeKnoraApiKey));
            vo.setKnowledgeBaseId(defaultWeKnoraKnowledgeBaseId);
            vo.setKnowledgeBaseName("CRM Default Knowledge Base");
            vo.setLlmProvider("aliyun");
            vo.setEmbeddingProvider("aliyun");
            vo.setEmbeddingDimension(1024);
            vo.setMatchCount(defaultWeKnoraMatchCount);
            vo.setVectorThreshold(defaultWeKnoraVectorThreshold);
            vo.setAutoRagEnabled(defaultWeKnoraAutoRagEnabled);
        }

        // 获取最后更新时间
        SystemConfig anyConfig = lambdaQuery()
                .eq(SystemConfig::getConfigType, WEKNORA_CONFIG_TYPE)
                .orderByDesc(SystemConfig::getUpdateTime)
                .last("LIMIT 1")
                .one();
        if (anyConfig != null) {
            vo.setUpdateTime(anyConfig.getUpdateTime());
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWeKnoraConfig(WeKnoraConfigUpdateBO updateBO) {
        Map<String, String> configs = new HashMap<>();

        if (updateBO.getEnabled() != null) {
            configs.put("weknora_enabled", String.valueOf(updateBO.getEnabled()));
        }
        if (StrUtil.isNotBlank(updateBO.getBaseUrl())) {
            configs.put("weknora_base_url", updateBO.getBaseUrl());
        }
        if (StrUtil.isNotBlank(updateBO.getApiKey()) && !isMaskedSecret(updateBO.getApiKey())) {
            configs.put("weknora_api_key", updateBO.getApiKey());
        }
        if (StrUtil.isNotBlank(updateBO.getKnowledgeBaseId())) {
            configs.put("weknora_knowledge_base_id", updateBO.getKnowledgeBaseId());
        }
        if (updateBO.getMatchCount() != null) {
            configs.put("weknora_match_count", String.valueOf(updateBO.getMatchCount()));
        }
        if (updateBO.getVectorThreshold() != null) {
            configs.put("weknora_vector_threshold", String.valueOf(updateBO.getVectorThreshold()));
        }
        if (updateBO.getAutoRagEnabled() != null) {
            configs.put("weknora_auto_rag_enabled", String.valueOf(updateBO.getAutoRagEnabled()));
        }

        updateConfigsWithType(configs, WEKNORA_CONFIG_TYPE);

        log.info("WeKnora 配置已更新");
    }

    @Override
    public WeKnoraConnectionTestVO testWeKnoraConnection(WeKnoraConfigUpdateBO configBO) {
        WeKnoraConnectionTestVO result = new WeKnoraConnectionTestVO();
        long startTime = System.currentTimeMillis();

        try {
            Map<String, String> currentConfigs = getConfigsByType(WEKNORA_CONFIG_TYPE);
            String baseUrl = normalizeBaseUrl(firstNotBlank(configBO.getBaseUrl(), currentConfigs.get("weknora_base_url"), defaultWeKnoraBaseUrl));
            String apiKey = ensureWeKnoraTenantApiKey(baseUrl, configBO.getApiKey(), currentConfigs.get("weknora_api_key"));

            if (StrUtil.isBlank(baseUrl) || StrUtil.isBlank(apiKey)) {
                result.setSuccess(false);
                result.setMessage("API 地址和 API Key 不能为空");
                result.setResponseTime(System.currentTimeMillis() - startTime);
                return result;
            }

            // 调用 WeKnora 健康检查或获取知识库信息
            RestTemplate restTemplate = new RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("X-API-Key", apiKey);
            org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(headers);

            // 尝试获取知识库列表
            String testUrl = baseUrl + "/knowledge-bases";
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    testUrl,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                result.setSuccess(true);
                result.setMessage("连接成功");
                // 可以解析返回值获取知识库数量
                result.setKnowledgeCount(0);
            } else {
                result.setSuccess(false);
                result.setMessage("连接失败: HTTP " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("WeKnora 连接测试失败: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("连接失败: " + extractErrorMessage(e));
        }

        result.setResponseTime(System.currentTimeMillis() - startTime);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WeKnoraModelSyncVO syncWeKnoraModels(WeKnoraModelSyncBO syncBO) {
        Map<String, String> currentConfigs = getConfigsByType(WEKNORA_CONFIG_TYPE);

        String baseUrl = normalizeBaseUrl(firstNotBlank(syncBO.getBaseUrl(), currentConfigs.get("weknora_base_url"), defaultWeKnoraBaseUrl));
        String apiKey = ensureWeKnoraTenantApiKey(baseUrl, syncBO.getApiKey(), currentConfigs.get("weknora_api_key"));
        String knowledgeBaseName = firstNotBlank(syncBO.getKnowledgeBaseName(), currentConfigs.get("weknora_knowledge_base_name"), "CRM Default Knowledge Base");

        String llmProvider = firstNotBlank(syncBO.getLlmProvider(), currentConfigs.get("weknora_llm_provider"), "aliyun");
        String llmName = required(firstNotBlank(syncBO.getLlmModelName(), currentConfigs.get("weknora_llm_model_name")), "LLM model name");
        String llmBaseUrl = required(firstNotBlank(syncBO.getLlmBaseUrl(), currentConfigs.get("weknora_llm_base_url")), "LLM base URL");
        String llmApiKey = resolveSecret(syncBO.getLlmApiKey(), currentConfigs.get("weknora_llm_api_key"), "LLM API Key");

        String embeddingProvider = firstNotBlank(syncBO.getEmbeddingProvider(), currentConfigs.get("weknora_embedding_provider"), "aliyun");
        String embeddingName = required(firstNotBlank(syncBO.getEmbeddingModelName(), currentConfigs.get("weknora_embedding_model_name")), "Embedding model name");
        String embeddingBaseUrl = required(firstNotBlank(syncBO.getEmbeddingBaseUrl(), currentConfigs.get("weknora_embedding_base_url")), "Embedding base URL");
        String embeddingApiKey = resolveSecret(syncBO.getEmbeddingApiKey(), currentConfigs.get("weknora_embedding_api_key"), "Embedding API Key");
        Integer embeddingDimension = syncBO.getEmbeddingDimension();
        if (embeddingDimension == null && StrUtil.isNotBlank(currentConfigs.get("weknora_embedding_dimension"))) {
            embeddingDimension = parseInt(currentConfigs.get("weknora_embedding_dimension"), 1024);
        }
        if (embeddingDimension == null || embeddingDimension <= 0) {
            throw new IllegalArgumentException("Embedding dimension is required");
        }

        String llmModelId = ensureWeKnoraModel(baseUrl, apiKey, "KnowledgeQA", llmName, llmProvider, llmBaseUrl, llmApiKey, null);
        String embeddingModelId = ensureWeKnoraModel(baseUrl, apiKey, "Embedding", embeddingName, embeddingProvider, embeddingBaseUrl, embeddingApiKey, embeddingDimension);
        String knowledgeBaseId = ensureWeKnoraKnowledgeBase(baseUrl, apiKey, knowledgeBaseName, llmModelId, embeddingModelId);

        Map<String, String> configs = new HashMap<>();
        configs.put("weknora_enabled", String.valueOf(syncBO.getEnabled() == null || syncBO.getEnabled()));
        configs.put("weknora_base_url", baseUrl);
        configs.put("weknora_api_key", apiKey);
        configs.put("weknora_knowledge_base_id", knowledgeBaseId);
        configs.put("weknora_knowledge_base_name", knowledgeBaseName);
        configs.put("weknora_llm_provider", llmProvider);
        configs.put("weknora_llm_model_name", llmName);
        configs.put("weknora_llm_base_url", llmBaseUrl);
        configs.put("weknora_llm_api_key", llmApiKey);
        configs.put("weknora_llm_model_id", llmModelId);
        configs.put("weknora_embedding_provider", embeddingProvider);
        configs.put("weknora_embedding_model_name", embeddingName);
        configs.put("weknora_embedding_base_url", embeddingBaseUrl);
        configs.put("weknora_embedding_api_key", embeddingApiKey);
        configs.put("weknora_embedding_dimension", String.valueOf(embeddingDimension));
        configs.put("weknora_embedding_model_id", embeddingModelId);
        updateConfigsWithType(configs, WEKNORA_CONFIG_TYPE);

        WeKnoraModelSyncVO result = new WeKnoraModelSyncVO();
        result.setSuccess(true);
        result.setMessage("WeKnora models and default knowledge base synced");
        result.setLlmModelId(llmModelId);
        result.setEmbeddingModelId(embeddingModelId);
        result.setKnowledgeBaseId(knowledgeBaseId);
        return result;
    }

    /**
     * 更新配置并指定配置类型
     */
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

            // 清除缓存
            redisTemplate.delete(CACHE_KEY_PREFIX + configKey);
        }
    }

    private boolean parseBoolean(String value, boolean defaultValue) {
        if (StrUtil.isBlank(value)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * API Key 脱敏处理
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

    private String getConfigValueOrDefault(Map<String, String> configs, String key, String defaultValue) {
        String value = configs.get(key);
        return StrUtil.isBlank(value) ? defaultValue : value;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value) && !isMaskedSecret(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String required(String value, String label) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value;
    }

    private String resolveSecret(String incoming, String stored, String label) {
        if (StrUtil.isNotBlank(incoming) && !isMaskedSecret(incoming)) {
            return incoming.trim();
        }
        if (StrUtil.isNotBlank(stored)) {
            return stored;
        }
        throw new IllegalArgumentException(label + " is required");
    }

    private boolean isMaskedSecret(String value) {
        return StrUtil.isNotBlank(value) && value.contains("****");
    }

    private String ensureWeKnoraTenantApiKey(String baseUrl, String incoming, String stored) {
        String apiKey = firstNotBlank(incoming, stored, defaultWeKnoraApiKey);
        if (StrUtil.isNotBlank(apiKey)) {
            return apiKey;
        }

        if (StrUtil.isBlank(weKnoraInitEmail) || StrUtil.isBlank(weKnoraInitPassword)) {
            throw new IllegalArgumentException("WeKnora tenant API Key is missing; configure WEKNORA_INIT_EMAIL and WEKNORA_INIT_PASSWORD or provide an existing key");
        }

        String generatedApiKey = loginOrRegisterWeKnoraTenant(baseUrl);
        Map<String, String> configs = new HashMap<>();
        configs.put("weknora_base_url", baseUrl);
        configs.put("weknora_api_key", generatedApiKey);
        updateConfigsWithType(configs, WEKNORA_CONFIG_TYPE);
        return generatedApiKey;
    }

    private String loginOrRegisterWeKnoraTenant(String baseUrl) {
        Map<String, Object> loginPayload = new LinkedHashMap<>();
        loginPayload.put("email", weKnoraInitEmail.trim());
        loginPayload.put("password", weKnoraInitPassword.trim());

        try {
            return tenantApiKey(weKnoraAuthRequest(baseUrl, "/auth/login", loginPayload));
        } catch (Exception loginError) {
            log.info("WeKnora login failed for configured init account; trying to register account");
            Map<String, Object> registerPayload = new LinkedHashMap<>();
            registerPayload.put("username", StrUtil.blankToDefault(weKnoraInitUsername, "admin"));
            registerPayload.put("email", weKnoraInitEmail.trim());
            registerPayload.put("password", weKnoraInitPassword.trim());
            weKnoraAuthRequest(baseUrl, "/auth/register", registerPayload);
            return tenantApiKey(weKnoraAuthRequest(baseUrl, "/auth/login", loginPayload));
        }
    }

    @SuppressWarnings("unchecked")
    private String tenantApiKey(Map<String, Object> session) {
        Object tenant = session.get("tenant");
        if (tenant instanceof Map<?, ?> tenantMap) {
            Object apiKey = ((Map<String, Object>) tenantMap).get("api_key");
            if (apiKey != null && StrUtil.isNotBlank(String.valueOf(apiKey))) {
                return String.valueOf(apiKey);
            }
        }
        throw new IllegalStateException("WeKnora tenant API Key was not returned by login");
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = required(baseUrl, "WeKnora API base URL").trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeProviderBaseUrl(String baseUrl) {
        String normalized = required(baseUrl, "model base URL").trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private HttpHeaders providerHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    @SuppressWarnings("unchecked")
    private void testLlmModel(WeKnoraModelTestVO result, String baseUrl, String apiKey, String modelName) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", "ping");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", modelName);
        payload.put("messages", List.of(message));
        payload.put("temperature", 0);
        payload.put("max_tokens", 8);

        ResponseEntity<Map> response = new RestTemplate().exchange(
                baseUrl + "/chat/completions",
                HttpMethod.POST,
                new HttpEntity<>(payload, providerHeaders(apiKey)),
                Map.class
        );
        Map<String, Object> body = response.getBody();
        Object choices = body == null ? null : body.get("choices");
        if (!response.getStatusCode().is2xxSuccessful() || !(choices instanceof List<?>) || ((List<?>) choices).isEmpty()) {
            throw new IllegalStateException("LLM response did not include choices");
        }
        result.setSuccess(true);
        result.setMessage("LLM 连接成功");
        result.setModel(modelName);
    }

    @SuppressWarnings("unchecked")
    private void testEmbeddingModel(WeKnoraModelTestVO result, String baseUrl, String apiKey, String modelName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", modelName);
        payload.put("input", List.of("ping"));

        ResponseEntity<Map> response = new RestTemplate().exchange(
                baseUrl + "/embeddings",
                HttpMethod.POST,
                new HttpEntity<>(payload, providerHeaders(apiKey)),
                Map.class
        );
        Map<String, Object> body = response.getBody();
        Object data = body == null ? null : body.get("data");
        if (!response.getStatusCode().is2xxSuccessful() || !(data instanceof List<?>) || ((List<?>) data).isEmpty()) {
            throw new IllegalStateException("Embedding response did not include data");
        }

        Object first = ((List<?>) data).get(0);
        Integer dimension = null;
        if (first instanceof Map<?, ?> item) {
            Object embedding = ((Map<String, Object>) item).get("embedding");
            if (embedding instanceof List<?>) {
                dimension = ((List<?>) embedding).size();
            }
        }
        result.setSuccess(true);
        result.setMessage("Embedding 连接成功");
        result.setModel(modelName);
        result.setEmbeddingDimension(dimension);
    }

    private HttpEntity<Object> weKnoraEntity(String apiKey, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", apiKey);
        return new HttpEntity<>(body, headers);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> weKnoraAuthRequest(String baseUrl, String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = new RestTemplate().exchange(
                baseUrl + path,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );
        Map<String, Object> responseBody = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || responseBody == null || Boolean.FALSE.equals(responseBody.get("success"))) {
            throw new IllegalStateException("WeKnora auth request failed: " + path);
        }
        return responseBody;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> weKnoraRequest(String baseUrl, String apiKey, HttpMethod method, String path, Object body) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + path,
                method,
                weKnoraEntity(apiKey, body),
                Map.class
        );
        Map<String, Object> responseBody = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || responseBody == null || Boolean.FALSE.equals(responseBody.get("success"))) {
            throw new IllegalStateException("WeKnora request failed: " + path);
        }
        return responseBody;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> weKnoraDataList(Map<String, Object> response) {
        Object data = response.get("data");
        if (data instanceof List<?>) {
            return ((List<?>) data).stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private String objectId(Map<String, Object> item) {
        Object id = item.get("id");
        return id == null ? "" : String.valueOf(id);
    }

    private String ensureWeKnoraModel(String baseUrl, String apiKey, String type, String name, String provider,
                                     String modelBaseUrl, String modelApiKey, Integer embeddingDimension) {
        for (Map<String, Object> model : weKnoraDataList(weKnoraRequest(baseUrl, apiKey, HttpMethod.GET, "/models", null))) {
            if (Objects.equals(type, String.valueOf(model.get("type"))) && Objects.equals(name, String.valueOf(model.get("name")))) {
                String existingId = objectId(model);
                if (StrUtil.isNotBlank(existingId)) {
                    return existingId;
                }
            }
        }

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("base_url", modelBaseUrl);
        parameters.put("api_key", modelApiKey);
        parameters.put("provider", provider);
        if (embeddingDimension != null) {
            Map<String, Object> embeddingParameters = new LinkedHashMap<>();
            embeddingParameters.put("dimension", embeddingDimension);
            embeddingParameters.put("truncate_prompt_tokens", 0);
            parameters.put("embedding_parameters", embeddingParameters);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("type", type);
        payload.put("source", "remote");
        payload.put("description", "Created by AI CRM model sync");
        payload.put("parameters", parameters);

        Map<String, Object> response = weKnoraRequest(baseUrl, apiKey, HttpMethod.POST, "/models", payload);
        Object data = response.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            String id = objectId((Map<String, Object>) dataMap);
            if (StrUtil.isNotBlank(id)) {
                return id;
            }
        }
        throw new IllegalStateException("Failed to create WeKnora model: " + name);
    }

    private String ensureWeKnoraKnowledgeBase(String baseUrl, String apiKey, String name, String llmModelId, String embeddingModelId) {
        for (Map<String, Object> kb : weKnoraDataList(weKnoraRequest(baseUrl, apiKey, HttpMethod.GET, "/knowledge-bases", null))) {
            if (Objects.equals(name, String.valueOf(kb.get("name")))) {
                String existingId = objectId(kb);
                if (StrUtil.isNotBlank(existingId)) {
                    return existingId;
                }
            }
        }

        Map<String, Object> chunkingConfig = new LinkedHashMap<>();
        chunkingConfig.put("chunk_size", 512);
        chunkingConfig.put("chunk_overlap", 100);
        chunkingConfig.put("separators", Arrays.asList("\n\n", "\n", ".", ";"));

        Map<String, Object> vlmConfig = new LinkedHashMap<>();
        vlmConfig.put("enabled", false);
        vlmConfig.put("model_id", "");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("description", "Default knowledge base for AI CRM documents");
        payload.put("type", "document");
        payload.put("embedding_model_id", embeddingModelId);
        payload.put("summary_model_id", llmModelId);
        payload.put("chunking_config", chunkingConfig);
        payload.put("vlm_config", vlmConfig);

        Map<String, Object> response = weKnoraRequest(baseUrl, apiKey, HttpMethod.POST, "/knowledge-bases", payload);
        Object data = response.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            String id = objectId((Map<String, Object>) dataMap);
            if (StrUtil.isNotBlank(id)) {
                return id;
            }
        }
        throw new IllegalStateException("Failed to create WeKnora knowledge base: " + name);
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

    /**
     * 根据 API URL 推断服务提供商
     */
    private String detectProvider(String apiUrl) {
        if (StrUtil.isBlank(apiUrl)) {
            return "dashscope";
        }
        if (apiUrl.contains("dashscope.aliyuncs.com")) {
            return "dashscope";
        }
        if (apiUrl.contains("api.openai.com")) {
            return "openai";
        }
        return "custom";
    }

    /**
     * 提取异常中的关键错误信息
     */
    private String extractErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return "未知错误";
        }
        // 处理常见错误
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
            return "无法连接到 API 服务器，请检查地址";
        }
        if (message.contains("timeout")) {
            return "连接超时";
        }
        // 截取关键信息
        if (message.length() > 100) {
            return message.substring(0, 100) + "...";
        }
        return message;
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

        // 获取最近更新时间
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
}

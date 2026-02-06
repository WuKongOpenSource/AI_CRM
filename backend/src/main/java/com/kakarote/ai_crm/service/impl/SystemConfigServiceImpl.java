package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import com.kakarote.ai_crm.entity.BO.AiConfigUpdateBO;
import com.kakarote.ai_crm.entity.BO.WeKnoraConfigUpdateBO;
import com.kakarote.ai_crm.entity.PO.SystemConfig;
import com.kakarote.ai_crm.entity.VO.AiConfigVO;
import com.kakarote.ai_crm.entity.VO.AiConnectionTestVO;
import com.kakarote.ai_crm.entity.VO.WeKnoraConfigVO;
import com.kakarote.ai_crm.entity.VO.WeKnoraConnectionTestVO;
import com.kakarote.ai_crm.mapper.SystemConfigMapper;
import com.kakarote.ai_crm.service.ISystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final long CACHE_EXPIRE_MINUTES = 30;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private DynamicChatClientProvider chatClientProvider;

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
        if (StrUtil.isNotBlank(updateBO.getApiKey())) {
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
            ChatClient testClient = chatClientProvider.createTestChatClient(
                    configBO.getApiUrl(),
                    configBO.getApiKey(),
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
            vo.setBaseUrl(configs.getOrDefault("weknora_base_url", defaultWeKnoraBaseUrl));
            vo.setApiKey(maskApiKey(configs.get("weknora_api_key")));
            vo.setKnowledgeBaseId(configs.getOrDefault("weknora_knowledge_base_id", defaultWeKnoraKnowledgeBaseId));
            vo.setMatchCount(parseInt(configs.get("weknora_match_count"), defaultWeKnoraMatchCount));
            vo.setVectorThreshold(parseDouble(configs.get("weknora_vector_threshold"), defaultWeKnoraVectorThreshold));
            vo.setAutoRagEnabled(parseBoolean(configs.get("weknora_auto_rag_enabled"), defaultWeKnoraAutoRagEnabled));
        } else {
            // 返回 application.yml 默认值
            vo.setEnabled(defaultWeKnoraEnabled);
            vo.setBaseUrl(defaultWeKnoraBaseUrl);
            vo.setApiKey(maskApiKey(defaultWeKnoraApiKey));
            vo.setKnowledgeBaseId(defaultWeKnoraKnowledgeBaseId);
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
        if (StrUtil.isNotBlank(updateBO.getApiKey())) {
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
            String baseUrl = configBO.getBaseUrl();
            String apiKey = configBO.getApiKey();

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
}

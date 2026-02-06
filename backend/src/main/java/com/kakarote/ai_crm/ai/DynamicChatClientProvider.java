package com.kakarote.ai_crm.ai;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.ai.tools.CustomerTools;
import com.kakarote.ai_crm.ai.tools.KnowledgeTools;
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
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 动态 ChatClient 提供者
 * 支持运行时热更新 AI 配置
 */
@Slf4j
@Component
public class DynamicChatClientProvider {

    private volatile ChatClient currentChatClient;
    private final Object lock = new Object();

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Autowired
    private CustomerTools customerTools;

    @Autowired
    private TaskTools taskTools;

    @Autowired
    private KnowledgeTools knowledgeTools;

    @Autowired
    private ToolCallingManager toolCallingManager;

    @Autowired(required = false)
    private ObservationRegistry observationRegistry;

    // 默认配置（从 application.yml 读取，作为后备）
    @Value("${spring.ai.openai.base-url:https://dashscope.aliyuncs.com/compatible-mode/}")
    private String defaultBaseUrl;

    @Value("${spring.ai.openai.api-key:}")
    private String defaultApiKey;

    @Value("${spring.ai.openai.chat.options.model:qwen-max}")
    private String defaultModel;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double defaultTemperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:2048}")
    private Integer defaultMaxTokens;

    /**
     * 获取当前生效的 ChatClient
     * 如果还没初始化，会自动从数据库加载配置创建
     */
    public ChatClient getChatClient() {
        if (currentChatClient == null) {
            synchronized (lock) {
                if (currentChatClient == null) {
                    refreshChatClient();
                }
            }
        }
        return currentChatClient;
    }

    /**
     * 刷新 ChatClient（配置变更后调用）
     */
    public void refreshChatClient() {
        synchronized (lock) {
            log.info("开始刷新 ChatClient...");

            // 从数据库加载 AI 配置
            Map<String, String> configs = loadAiConfigsFromDB();

            String apiUrl = configs.getOrDefault("ai_api_url", defaultBaseUrl);
            String apiKey = configs.getOrDefault("ai_api_key", defaultApiKey);
            String model = configs.getOrDefault("ai_model", defaultModel);
            double temperature = parseDouble(configs.get("ai_temperature"), defaultTemperature);
            int maxTokens = parseInt(configs.get("ai_max_tokens"), defaultMaxTokens);

            if (StrUtil.isBlank(apiKey)) {
                log.warn("AI API Key 未配置，ChatClient 将使用默认配置");
                apiKey = defaultApiKey;
            }

            if (StrUtil.isBlank(apiKey)) {
                log.error("AI API Key 未配置且无默认值，ChatClient 将无法正常工作");
            }

            currentChatClient = createChatClient(apiUrl, apiKey, model, temperature, maxTokens);

            log.info("ChatClient 刷新完成: baseUrl={}, model={}", apiUrl, model);
        }
    }

    /**
     * 创建 ChatClient 实例
     * 用于正式使用和测试连接
     */
    public ChatClient createChatClient(String baseUrl, String apiKey, String model,
                                        Double temperature, Integer maxTokens) {
        // 创建 OpenAiApi
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();

        // 创建 ChatModel 配置
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();

        // 创建 ChatModel（Spring AI 1.0.0 需要 5 个参数）
        ObservationRegistry obsRegistry = observationRegistry != null
                ? observationRegistry : ObservationRegistry.NOOP;
        OpenAiChatModel chatModel = new OpenAiChatModel(
                openAiApi, options, toolCallingManager,
                RetryTemplate.builder().build(), obsRegistry);

        // 创建 ChatClient 并注册工具
        return ChatClient.builder(chatModel)
                .defaultTools(customerTools, taskTools, knowledgeTools)
                .build();
    }

    /**
     * 创建用于测试连接的简单 ChatClient（不注册工具）
     */
    public ChatClient createTestChatClient(String baseUrl, String apiKey, String model,
                                            Double temperature, Integer maxTokens) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature != null ? temperature : 0.7)
                .maxTokens(maxTokens != null ? maxTokens : 100) // 测试用小 token
                .build();

        ObservationRegistry obsRegistry = observationRegistry != null
                ? observationRegistry : ObservationRegistry.NOOP;
        OpenAiChatModel chatModel = new OpenAiChatModel(
                openAiApi, options, toolCallingManager,
                RetryTemplate.builder().build(), obsRegistry);

        return ChatClient.builder(chatModel).build();
    }

    /**
     * 从数据库加载 AI 配置
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
     * 应用启动时初始化
     */
    @PostConstruct
    public void init() {
        try {
            refreshChatClient();
        } catch (Exception e) {
            log.warn("初始化 ChatClient 失败（可能配置尚未设置）: {}", e.getMessage());
        }
    }
}

package com.kakarote.ai_crm.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * WeKnora 配置类
 * 用于配置 WeKnora 知识库服务的连接信息
 * 支持从数据库动态读取配置，优先使用数据库配置，fallback 到 application.yml
 */
@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "weknora")
public class WeKnoraConfig {

    private static final String CACHE_KEY_PREFIX = "system:config:";

    @Autowired
    @Lazy
    private StringRedisTemplate redisTemplate;

    /**
     * WeKnora API 基础 URL
     * 例如: http://localhost:8080/api/v1
     */
    private String baseUrl = "http://localhost:8080/api/v1";

    /**
     * WeKnora API Key
     * 用于身份认证，从 WeKnora 管理界面获取
     */
    private String apiKey;

    /**
     * 默认知识库 ID
     * CRM 文档将上传到此知识库
     */
    private String knowledgeBaseId;

    /**
     * 是否启用 WeKnora 集成
     * 设为 false 可禁用 WeKnora 功能，仅使用本地存储
     */
    private boolean enabled = false;

    /**
     * 搜索相关配置
     */
    private SearchConfig search = new SearchConfig();

    @Data
    public static class SearchConfig {
        /**
         * 检索返回的最大结果数
         */
        private int matchCount = 5;

        /**
         * 向量相似度阈值 (0-1)
         * 只返回相似度高于此阈值的结果
         */
        private double vectorThreshold = 0.5;

        /**
         * 是否启用自动 RAG
         * 启用后每次对话都会自动检索相关文档
         */
        private boolean autoRagEnabled = true;
    }

    // ==================== 动态配置读取方法 ====================

    /**
     * 获取是否启用（优先从数据库读取）
     */
    public boolean isEnabled() {
        String dbValue = getDbConfigValue("weknora_enabled");
        if (dbValue != null) {
            return Boolean.parseBoolean(dbValue);
        }
        return enabled;
    }

    /**
     * 获取 API 基础 URL（优先从数据库读取）
     */
    public String getBaseUrl() {
        String dbValue = getDbConfigValue("weknora_base_url");
        if (StrUtil.isNotBlank(dbValue)) {
            return dbValue;
        }
        return baseUrl;
    }

    /**
     * 获取 API Key（优先从数据库读取）
     */
    public String getApiKey() {
        String dbValue = getDbConfigValue("weknora_api_key");
        if (StrUtil.isNotBlank(dbValue)) {
            return dbValue;
        }
        return apiKey;
    }

    /**
     * 获取知识库 ID（优先从数据库读取）
     */
    public String getKnowledgeBaseId() {
        String dbValue = getDbConfigValue("weknora_knowledge_base_id");
        if (StrUtil.isNotBlank(dbValue)) {
            return dbValue;
        }
        return knowledgeBaseId;
    }

    /**
     * 获取搜索配置（优先从数据库读取）
     */
    public SearchConfig getSearch() {
        SearchConfig config = new SearchConfig();

        String matchCount = getDbConfigValue("weknora_match_count");
        if (StrUtil.isNotBlank(matchCount)) {
            try {
                config.setMatchCount(Integer.parseInt(matchCount));
            } catch (NumberFormatException e) {
                config.setMatchCount(search.getMatchCount());
            }
        } else {
            config.setMatchCount(search.getMatchCount());
        }

        String vectorThreshold = getDbConfigValue("weknora_vector_threshold");
        if (StrUtil.isNotBlank(vectorThreshold)) {
            try {
                config.setVectorThreshold(Double.parseDouble(vectorThreshold));
            } catch (NumberFormatException e) {
                config.setVectorThreshold(search.getVectorThreshold());
            }
        } else {
            config.setVectorThreshold(search.getVectorThreshold());
        }

        String autoRagEnabled = getDbConfigValue("weknora_auto_rag_enabled");
        if (autoRagEnabled != null) {
            config.setAutoRagEnabled(Boolean.parseBoolean(autoRagEnabled));
        } else {
            config.setAutoRagEnabled(search.isAutoRagEnabled());
        }

        return config;
    }

    /**
     * 从 Redis 缓存获取数据库配置值
     */
    private String getDbConfigValue(String key) {
        try {
            if (redisTemplate == null) {
                return null;
            }
            return redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + key);
        } catch (Exception e) {
            log.debug("从 Redis 读取配置失败: {}", e.getMessage());
            return null;
        }
    }
}

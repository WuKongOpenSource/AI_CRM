package com.kakarote.ai_crm.config;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.entity.PO.SystemConfig;
import com.kakarote.ai_crm.mapper.SystemConfigMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * WeKnora 配置类 - 支持从数据库动态读取配置，fallback 到 application.yml
 */
@Setter
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "weknora")
public class WeKnoraConfig {

    private static final String CACHE_KEY_PREFIX = "system:config:";
    private static final long CACHE_EXPIRE_MINUTES = 30;

    @Autowired
    @Lazy
    private StringRedisTemplate redisTemplate;

    @Autowired
    @Lazy
    private SystemConfigMapper systemConfigMapper;

    private String baseUrl = "http://localhost:8080/api/v1";
    private String apiKey;
    private String knowledgeBaseId;
    private boolean enabled = false;
    private SearchConfig search = new SearchConfig();

    @Setter
    public static class SearchConfig {
        private int matchCount = 5;
        private double vectorThreshold = 0.5;
        private boolean autoRagEnabled = true;

        public int getMatchCount() { return matchCount; }
        public double getVectorThreshold() { return vectorThreshold; }
        public boolean isAutoRagEnabled() { return autoRagEnabled; }
    }

    // getter 优先从数据库读取，fallback 到 application.yml

    public boolean isEnabled() {
        String dbValue = getDbConfigValue("weknora_enabled");
        if (dbValue != null) {
            return Boolean.parseBoolean(dbValue);
        }
        return enabled;
    }

    public String getBaseUrl() {
        String dbValue = getDbConfigValue("weknora_base_url");
        if (StrUtil.isNotBlank(dbValue)) {
            return dbValue;
        }
        return baseUrl;
    }

    public String getApiKey() {
        String dbValue = getDbConfigValue("weknora_api_key");
        if (StrUtil.isNotBlank(dbValue)) {
            return dbValue;
        }
        return apiKey;
    }

    public String getKnowledgeBaseId() {
        String dbValue = getDbConfigValue("weknora_knowledge_base_id");
        if (StrUtil.isNotBlank(dbValue)) {
            return dbValue;
        }
        return knowledgeBaseId;
    }

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

    private String getDbConfigValue(String key) {
        try {
            if (redisTemplate != null) {
                String cachedValue = redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + key);
                if (cachedValue != null) {
                    return cachedValue;
                }
            }
            if (systemConfigMapper != null) {
                LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(SystemConfig::getConfigKey, key);
                SystemConfig config = systemConfigMapper.selectOne(wrapper);
                if (config != null && StrUtil.isNotBlank(config.getConfigValue())) {
                    if (redisTemplate != null) {
                        try {
                            redisTemplate.opsForValue().set(CACHE_KEY_PREFIX + key,
                                    config.getConfigValue(), CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                        } catch (Exception ignored) {}
                    }
                    return config.getConfigValue();
                }
            }
            return null;
        } catch (Exception e) {
            log.debug("读取动态配置失败: {}", e.getMessage());
            return null;
        }
    }
}

package com.kakarote.ai_crm.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.config.WeKnoraConfig;
import com.kakarote.ai_crm.entity.PO.CrmTenant;
import com.kakarote.ai_crm.entity.VO.WeKnoraChunk;
import com.kakarote.ai_crm.entity.VO.WeKnoraKnowledge;
import com.kakarote.ai_crm.mapper.CrmTenantMapper;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * WeKnora REST 客户端
 * 支持多租户隔离：每个 CRM 租户对应一个 WeKnora 租户（独立 API Key）
 */
@Slf4j
@Service
public class WeKnoraClient {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "txt", "md", "markdown",
            "xls", "xlsx", "csv",
            "html", "htm", "json", "xml"
    );

    private final RestTemplate restTemplate;
    private final WeKnoraConfig config;
    private final ObjectMapper objectMapper;
    private final CrmTenantMapper crmTenantMapper;
    private final ConcurrentHashMap<Long, Object> tenantLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> conversationSessionCache = new ConcurrentHashMap<>();

    @Autowired
    public WeKnoraClient(WeKnoraConfig config, CrmTenantMapper crmTenantMapper) {
        this(config, crmTenantMapper, new RestTemplate(), new ObjectMapper());
    }

    WeKnoraClient(WeKnoraConfig config, CrmTenantMapper crmTenantMapper, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.config = config;
        this.crmTenantMapper = crmTenantMapper;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean isSupportedFileType(String filename) {
        if (filename == null || !filename.contains(".")) return false;
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return SUPPORTED_EXTENSIONS.contains(ext);
    }

    public Set<String> getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    // ==================== 租户上下文（核心） ====================

    /**
     * 租户的 WeKnora 上下文（API Key + 知识库 ID）
     */
    @Data
    public static class TenantWeKnoraContext {
        private final String apiKey;
        private final String knowledgeBaseId;
    }

    @Data
    private static class RagModelContext {
        private final String summaryModelId;
        private final String embeddingModelId;
    }

    @Data
    private static class TenantModelInfo {
        private final String id;
        private final String name;
        private final String type;
    }

    @Data
    public static class WeKnoraChatResult {
        private final String answer;
        private final List<WeKnoraChunk> references;
        private final boolean completed;
    }

    /**
     * 获取或创建租户的 WeKnora 上下文（懒初始化）
     * 流程：注册 WeKnora 租户 → 创建 Embedding 模型 → 创建知识库
     */
    public TenantWeKnoraContext getOrCreateTenantContext(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId 不能为空");
        }

        CrmTenant tenant = crmTenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new IllegalStateException("租户不存在: " + tenantId);
        }

        // 快速路径：已完成初始化
        if (StrUtil.isNotBlank(tenant.getWeKnoraApiKey()) && StrUtil.isNotBlank(tenant.getWeKnoraKnowledgeBaseId())) {
            return new TenantWeKnoraContext(tenant.getWeKnoraApiKey(), tenant.getWeKnoraKnowledgeBaseId());
        }

        // 需要初始化 — 加锁防并发
        Object lock = tenantLocks.computeIfAbsent(tenantId, k -> new Object());
        synchronized (lock) {
            try {
                // double-check
                tenant = crmTenantMapper.selectById(tenantId);
                if (StrUtil.isNotBlank(tenant.getWeKnoraApiKey()) && StrUtil.isNotBlank(tenant.getWeKnoraKnowledgeBaseId())) {
                    return new TenantWeKnoraContext(tenant.getWeKnoraApiKey(), tenant.getWeKnoraKnowledgeBaseId());
                }

                String apiKey = tenant.getWeKnoraApiKey();
                String kbId = tenant.getWeKnoraKnowledgeBaseId();

                // Step 1: 注册 WeKnora 租户（如果没有 API Key）
                if (StrUtil.isBlank(apiKey)) {
                    String email = "tenant_" + tenantId + "@crm.internal";
                    String password = "CrmTenant!" + tenantId;
                    String username = "tenant_" + tenantId;
                    apiKey = registerWeKnoraTenant(email, password, username);

                    LambdaUpdateWrapper<CrmTenant> update = new LambdaUpdateWrapper<>();
                    update.eq(CrmTenant::getTenantId, tenantId).set(CrmTenant::getWeKnoraApiKey, apiKey);
                    crmTenantMapper.update(null, update);
                    log.info("WeKnora 租户注册成功: tenantId={}", tenantId);
                }

                // Step 2: 创建 Embedding 模型 + 知识库（如果没有 KB ID）
                if (StrUtil.isBlank(kbId)) {
                    RagModelContext modelContext = ensureDefaultRagModels(apiKey);
                    kbId = createKnowledgeBase(
                            apiKey,
                            "default",
                            modelContext.getEmbeddingModelId(),
                            modelContext.getSummaryModelId()
                    );

                    LambdaUpdateWrapper<CrmTenant> update = new LambdaUpdateWrapper<>();
                    update.eq(CrmTenant::getTenantId, tenantId).set(CrmTenant::getWeKnoraKnowledgeBaseId, kbId);
                    crmTenantMapper.update(null, update);
                    log.info("WeKnora 知识库创建成功: tenantId={}, kbId={}", tenantId, kbId);
                }

                return new TenantWeKnoraContext(apiKey, kbId);
            } finally {
                tenantLocks.remove(tenantId);
            }
        }
    }

    // ==================== WeKnora 租户注册 ====================

    /**
     * 在 WeKnora 中注册租户，返回 API Key
     * POST /auth/register（无需认证）
     */
    private String registerWeKnoraTenant(String email, String password, String username) {
        RuntimeException createTenantError = null;
        if (StrUtil.isNotBlank(config.getApiKey())) {
            try {
                return createTenantWithGlobalApiKey(username);
            } catch (RuntimeException e) {
                createTenantError = e;
                log.warn("WeKnora /tenants 创建租户失败，回退到认证接口: {}", e.getMessage());
            }
        }

        try {
            try {
                return registerWeKnoraTenantLegacy(email, password, username);
            } catch (RuntimeException legacyError) {
                log.info("WeKnora /auth/register 未直接返回 api_key，继续通过登录接口获取: {}", legacyError.getMessage());
                try {
                    return loginAndGetTenantApiKey(email, password);
                } catch (Exception loginAfterRegisterError) {
                    log.info("WeKnora /auth/login 未能直接取回 api_key，继续补做 register+login: {}", loginAfterRegisterError.getMessage());
                }
            }

            registerUser(email, password, username);
            return loginAndGetTenantApiKey(email, password);
        } catch (Exception e) {
            log.error("WeKnora 租户注册异常: {}", e.getMessage(), e);
            if (createTenantError != null) {
                throw new RuntimeException(
                        "WeKnora 租户注册失败: /tenants=" + createTenantError.getMessage()
                                + "; /auth/register+/auth/login=" + e.getMessage(),
                        e
                );
            }
            throw new RuntimeException("WeKnora 租户注册失败: " + e.getMessage(), e);
        }
    }

    private String createTenantWithGlobalApiKey(String tenantName) {
        String url = config.getBaseUrl() + "/tenants";

        HttpHeaders headers = createHeaders(config.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", tenantName);
        body.put("description", "AI CRM tenant " + tenantName);
        body.put("business", "crm");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String apiKey = extractApiKey(root);
                if (StrUtil.isNotBlank(apiKey)) {
                    return apiKey;
                }
                throw new RuntimeException("WeKnora /tenants 创建成功但未返回 api_key: " + response.getBody());
            }
            throw new RuntimeException("WeKnora /tenants 创建租户失败: " + response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("WeKnora /tenants 创建租户失败: " + e.getMessage(), e);
        }
    }

    private void registerUser(String email, String password, String username) throws IOException {
        String url = config.getBaseUrl() + "/auth/register";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("email", email, "password", password, "username", username);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.path("success").asBoolean(false)) {
                    return;
                }
            }
            throw new RuntimeException("WeKnora /auth/register 失败: " + response.getBody());
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            if (containsAlreadyExists(responseBody)) {
                log.info("WeKnora 用户已存在，继续尝试登录获取租户 API Key: {}", email);
                return;
            }
            throw new RuntimeException("WeKnora /auth/register 失败: " + responseBody, e);
        }
    }

    private String loginAndGetTenantApiKey(String email, String password) throws IOException {
        String url = config.getBaseUrl() + "/auth/login";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("email", email, "password", password);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("WeKnora /auth/login 失败: " + response.getBody());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            if (!root.path("success").asBoolean(false)) {
                throw new RuntimeException("WeKnora /auth/login 失败: " + response.getBody());
            }

            String apiKey = extractApiKey(root);
            if (StrUtil.isNotBlank(apiKey)) {
                return apiKey;
            }

            String token = readText(root, "token");
            if (StrUtil.isNotBlank(token)) {
                apiKey = getCurrentTenantApiKey(token);
                if (StrUtil.isNotBlank(apiKey)) {
                    return apiKey;
                }
            }

            throw new RuntimeException("WeKnora /auth/login 成功但未返回 tenant.api_key: " + response.getBody());
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            throw new RuntimeException("WeKnora /auth/login 失败: " + e.getResponseBodyAsString(), e);
        }
    }

    private String getCurrentTenantApiKey(String token) throws IOException {
        String url = config.getBaseUrl() + "/auth/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            return extractApiKey(root);
        }
        return null;
    }

    private boolean containsAlreadyExists(String responseBody) {
        if (StrUtil.isBlank(responseBody)) {
            return false;
        }
        String normalized = responseBody.toLowerCase(Locale.ROOT);
        return normalized.contains("already exists") || normalized.contains("已存在");
    }

    private String extractApiKey(JsonNode root) {
        return firstNonBlank(
                readText(root, "data", "api_key"),
                readText(root, "tenant", "api_key"),
                readText(root, "data", "tenant", "api_key"),
                readText(root, "api_key")
        );
    }

    private String readText(JsonNode root, String... path) {
        JsonNode node = root;
        for (String name : path) {
            if (node == null || node.isMissingNode() || node.isNull()) {
                return null;
            }
            node = node.path(name);
        }
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return StrUtil.isBlank(value) ? null : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private RagModelContext ensureDefaultRagModels(String tenantApiKey) {
        List<TenantModelInfo> models = listModels(tenantApiKey);

        String summaryModelId = firstNonBlank(
                findModelId(models, "KnowledgeQA", config.getInitModels().getChat().getName()),
                findFirstModelIdByType(models, "KnowledgeQA")
        );
        if (StrUtil.isBlank(summaryModelId)) {
            summaryModelId = createKnowledgeQaModel(tenantApiKey);
            log.info("WeKnora 默认 KnowledgeQA 模型创建成功: modelId={}", summaryModelId);
        }

        String embeddingModelId = firstNonBlank(
                findModelId(models, "Embedding", config.getInitModels().getEmbedding().getName()),
                findFirstModelIdByType(models, "Embedding")
        );
        if (StrUtil.isBlank(embeddingModelId)) {
            embeddingModelId = createEmbeddingModel(tenantApiKey);
        }

        return new RagModelContext(summaryModelId, embeddingModelId);
    }

    private List<TenantModelInfo> listModels(String tenantApiKey) {
        String url = config.getBaseUrl() + "/models";

        HttpEntity<Void> requestEntity = new HttpEntity<>(createHeaders(tenantApiKey));

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.path("data");
            if (!data.isArray()) {
                return Collections.emptyList();
            }

            List<TenantModelInfo> models = new ArrayList<>();
            for (JsonNode item : data) {
                String id = readText(item, "id");
                String name = readText(item, "name");
                String type = readText(item, "type");
                if (StrUtil.isNotBlank(id) && StrUtil.isNotBlank(type)) {
                    models.add(new TenantModelInfo(id, name, type));
                }
            }
            return models;
        } catch (Exception e) {
            log.warn("WeKnora 获取模型列表失败，将尝试直接创建默认模型: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String findModelId(List<TenantModelInfo> models, String type, String modelName) {
        if (StrUtil.isBlank(modelName)) {
            return null;
        }
        return models.stream()
                .filter(model -> type.equalsIgnoreCase(model.getType()))
                .filter(model -> modelName.equalsIgnoreCase(model.getName()))
                .map(TenantModelInfo::getId)
                .findFirst()
                .orElse(null);
    }

    private String findFirstModelIdByType(List<TenantModelInfo> models, String type) {
        return models.stream()
                .filter(model -> type.equalsIgnoreCase(model.getType()))
                .map(TenantModelInfo::getId)
                .findFirst()
                .orElse(null);
    }

    private String createKnowledgeQaModel(String tenantApiKey) {
        WeKnoraConfig.InitModels.ChatModel chatCfg = config.getInitModels().getChat();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("api_key", resolveInitModelApiKey(chatCfg.getApiKey()));
        parameters.put("base_url", chatCfg.getBaseUrl());
        parameters.put("provider", chatCfg.getProvider());

        return createModel(
                tenantApiKey,
                chatCfg.getName(),
                "KnowledgeQA",
                chatCfg.getSource(),
                "AI CRM RAG chat model",
                parameters
        );
    }

    private String createModel(String tenantApiKey, String name, String type, String source,
                               String description, Map<String, Object> parameters) {
        String url = config.getBaseUrl() + "/models";

        HttpHeaders headers = createHeaders(tenantApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("type", type);
        body.put("source", source);
        body.put("description", description);
        body.put("parameters", parameters);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("data") && root.get("data").has("id")) {
                    return root.get("data").get("id").asText();
                }
                if (root.has("id")) {
                    return root.get("id").asText();
                }
            }
            throw new RuntimeException("WeKnora 创建模型失败: " + response.getBody());
        } catch (Exception e) {
            log.error("WeKnora 创建模型异常: type={}, name={}, error={}", type, name, e.getMessage(), e);
            throw new RuntimeException("WeKnora 创建模型失败: " + e.getMessage(), e);
        }
    }

    private String resolveInitModelApiKey(String configuredApiKey) {
        return firstNonBlank(configuredApiKey, System.getenv("DASHSCOPE_API_KEY"));
    }

    private String registerWeKnoraTenantLegacy(String email, String password, String username) {
        String url = config.getBaseUrl() + "/auth/register";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("email", email, "password", password, "username", username);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("tenant") && root.get("tenant").has("api_key")) {
                    return root.get("tenant").get("api_key").asText();
                }
            }
            throw new RuntimeException("WeKnora 注册失败: " + response.getBody());
        } catch (Exception e) {
            log.error("WeKnora 租户注册异常: {}", e.getMessage(), e);
            throw new RuntimeException("WeKnora 租户注册失败: " + e.getMessage(), e);
        }
    }

    // ==================== 模型管理 ====================

    /**
     * 为租户创建 Embedding 模型，返回 model ID
     * POST /models（需租户 X-API-Key）
     */
    private String createEmbeddingModel(String tenantApiKey) {
        WeKnoraConfig.InitModels.EmbeddingModel embCfg = config.getInitModels().getEmbedding();

        Map<String, Object> embeddingParams = new HashMap<>();
        embeddingParams.put("dimension", embCfg.getDimension());

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("api_key", resolveInitModelApiKey(embCfg.getApiKey()));
        parameters.put("base_url", embCfg.getBaseUrl());
        parameters.put("provider", embCfg.getProvider());
        parameters.put("embedding_parameters", embeddingParams);

        String modelId = createModel(
                tenantApiKey,
                embCfg.getName(),
                "Embedding",
                embCfg.getSource(),
                "AI CRM RAG embedding model",
                parameters
        );
        log.info("WeKnora Embedding 模型创建成功: modelId={}", modelId);
        return modelId;
    }

    private String createEmbeddingModelLegacy(String tenantApiKey) {
        String url = config.getBaseUrl() + "/models";

        HttpHeaders headers = createHeaders(tenantApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        WeKnoraConfig.InitModels.EmbeddingModel embCfg = config.getInitModels().getEmbedding();

        Map<String, Object> embeddingParams = new HashMap<>();
        embeddingParams.put("dimension", embCfg.getDimension());

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("api_key", resolveInitModelApiKey(embCfg.getApiKey()));
        parameters.put("base_url", embCfg.getBaseUrl());
        parameters.put("provider", embCfg.getProvider());
        parameters.put("embedding_parameters", embeddingParams);

        Map<String, Object> body = new HashMap<>();
        body.put("name", embCfg.getName());
        body.put("type", "Embedding");
        body.put("source", embCfg.getSource());
        body.put("parameters", parameters);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("data") && root.get("data").has("id")) {
                    String modelId = root.get("data").get("id").asText();
                    log.info("WeKnora Embedding 模型创建成功: modelId={}", modelId);
                    return modelId;
                }
                // 有些 API 直接返回 id
                if (root.has("id")) {
                    return root.get("id").asText();
                }
            }
            throw new RuntimeException("WeKnora 创建 Embedding 模型失败: " + response.getBody());
        } catch (Exception e) {
            log.error("WeKnora 创建 Embedding 模型异常: {}", e.getMessage(), e);
            throw new RuntimeException("WeKnora 创建 Embedding 模型失败: " + e.getMessage(), e);
        }
    }

    // ==================== 知识库管理 ====================

    /**
     * 创建知识库，返回 KB ID
     * POST /knowledge-bases（需租户 X-API-Key）
     */
    public String createKnowledgeBase(String tenantApiKey, String name, String embeddingModelId, String summaryModelId) {
        String url = config.getBaseUrl() + "/knowledge-bases";

        HttpHeaders headers = createHeaders(tenantApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("embedding_model_id", embeddingModelId);
        if (StrUtil.isNotBlank(summaryModelId)) {
            body.put("summary_model_id", summaryModelId);
        }
        body.put("type", "document");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("data") && root.get("data").has("id")) {
                    return root.get("data").get("id").asText();
                }
                if (root.has("id")) {
                    return root.get("id").asText();
                }
            }
            throw new RuntimeException("WeKnora 创建知识库失败: " + response.getBody());
        } catch (Exception e) {
            log.error("WeKnora 创建知识库异常: {}", e.getMessage(), e);
            throw new RuntimeException("WeKnora 创建知识库失败: " + e.getMessage(), e);
        }
    }

    // ==================== 文档上传 ====================

    public WeKnoraKnowledge uploadDocument(String filePath, String originalFilename, String knowledgeBaseId, String tenantApiKey) throws IOException {
        File file = new File(filePath);
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        return uploadDocument(fileBytes, originalFilename, knowledgeBaseId, tenantApiKey);
    }

    public WeKnoraKnowledge uploadDocument(byte[] fileBytes, String originalFilename, String knowledgeBaseId, String tenantApiKey) throws IOException {
        if (!isEnabled()) {
            log.debug("WeKnora 未启用，跳过上传");
            return null;
        }

        String url = config.getBaseUrl() + "/knowledge-bases/" + knowledgeBaseId + "/knowledge/file";

        HttpHeaders headers = createHeaders(tenantApiKey);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return originalFilename;
            }
        });
        body.add("enable_multimodel", "true");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("success") && root.get("success").asBoolean() && root.has("data")) {
                    return objectMapper.treeToValue(root.get("data"), WeKnoraKnowledge.class);
                }
            }
            log.error("WeKnora 上传失败: {}", response.getBody());
            return null;
        } catch (org.springframework.web.client.HttpClientErrorException.Conflict e) {
            log.info("WeKnora 文件已存在，使用已有记录: {}", originalFilename);
            try {
                JsonNode root = objectMapper.readTree(e.getResponseBodyAsString());
                if (root.has("data")) {
                    return objectMapper.treeToValue(root.get("data"), WeKnoraKnowledge.class);
                }
            } catch (Exception parseEx) {
                log.warn("解析重复文件响应失败: {}", parseEx.getMessage());
            }
            return null;
        } catch (Exception e) {
            log.error("WeKnora 上传异常: {}", e.getMessage(), e);
            throw new RuntimeException("WeKnora 上传失败: " + e.getMessage(), e);
        }
    }

    // ==================== 删除 ====================

    public void deleteKnowledge(String weKnoraKnowledgeId, String tenantApiKey) {
        if (!isEnabled() || weKnoraKnowledgeId == null) return;

        String url = config.getBaseUrl() + "/knowledge/" + weKnoraKnowledgeId;
        HttpEntity<?> requestEntity = new HttpEntity<>(createHeaders(tenantApiKey));

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("WeKnora 知识删除成功: {}", weKnoraKnowledgeId);
            } else {
                log.warn("WeKnora 知识删除失败: {} - {}", weKnoraKnowledgeId, response.getBody());
            }
        } catch (Exception e) {
            log.error("WeKnora 删除异常: {}", e.getMessage(), e);
        }
    }

    // ==================== 搜索 ====================

    public List<WeKnoraChunk> searchKnowledge(String query, String knowledgeBaseId, String tenantApiKey) {
        if (!isEnabled()) return Collections.emptyList();

        String url = config.getBaseUrl() + "/knowledge-search";

        HttpHeaders headers = createHeaders(tenantApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", query);
        requestBody.put("knowledge_base_id", knowledgeBaseId);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("success") && root.get("success").asBoolean() && root.has("data")) {
                    List<WeKnoraChunk> chunks = objectMapper.convertValue(
                            root.get("data"), new TypeReference<List<WeKnoraChunk>>() {});
                    double threshold = config.getSearch().getVectorThreshold();
                    int maxCount = config.getSearch().getMatchCount();
                    List<WeKnoraChunk> filteredChunks = chunks.stream()
                            .filter(c -> c.getScore() == null || c.getScore() >= threshold)
                            .limit(maxCount)
                            .toList();

                    if (!filteredChunks.isEmpty()) {
                        return filteredChunks;
                    }

                    if (!chunks.isEmpty()) {
                        log.warn("RAG 检索结果被阈值全部过滤，回退为 Top{} 原始结果: threshold={}, query={}",
                                maxCount, threshold, query);
                        return chunks.stream()
                                .limit(maxCount)
                                .toList();
                    }
                    return Collections.emptyList();
                }
            }
            log.warn("WeKnora 搜索返回空结果: {}", response.getBody());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("WeKnora 搜索异常: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public WeKnoraChatResult askKnowledgeQuestion(Long tenantId, Long conversationId, String query) {
        return askKnowledgeQuestion(tenantId, conversationId, query, Collections.emptyList());
    }

    public WeKnoraChatResult askKnowledgeQuestion(Long tenantId, Long conversationId,
                                                  String query, List<String> knowledgeIds) {
        if (!isEnabled() || StrUtil.isBlank(query)) {
            return new WeKnoraChatResult("", Collections.emptyList(), false);
        }

        TenantWeKnoraContext ctx = getOrCreateTenantContext(tenantId);
        String sessionId = getOrCreateConversationSession(tenantId, conversationId, ctx);
        boolean useAgentChat = knowledgeIds != null && !knowledgeIds.isEmpty();
        log.debug("RAG问答请求开始: tenantId={}, conversationId={}, sessionId={}, kbId={}, useAgentChat={}, knowledgeIds={}, query={}",
                tenantId, conversationId, sessionId, ctx.getKnowledgeBaseId(),
                useAgentChat, knowledgeIds, abbreviateForLog(query));
        try {
            WeKnoraChatResult result = executeKnowledgeChat(ctx, sessionId, query, knowledgeIds, useAgentChat);
            if (useAgentChat && isUnavailableChatResult(result)) {
                log.warn("WeKnora 定向知识问答未返回可用结果，降级为通用知识库问答: tenantId={}, sessionId={}",
                        tenantId, sessionId);
                result = executeKnowledgeChat(ctx, sessionId, query, Collections.emptyList(), false);
            }
            log.debug("RAG问答请求完成: tenantId={}, conversationId={}, sessionId={}, answerLength={}, references={}, completed={}",
                    tenantId, conversationId, sessionId,
                    result.getAnswer() != null ? result.getAnswer().length() : 0,
                    result.getReferences() != null ? result.getReferences().size() : 0,
                    result.isCompleted());
            return result;
        } catch (Exception e) {
            log.error("WeKnora 问答异常: tenantId={}, sessionId={}, error={}",
                    tenantId, sessionId, e.getMessage(), e);
            return new WeKnoraChatResult("", Collections.emptyList(), false);
        }
    }

    public void clearConversationSession(Long tenantId, Long conversationId) {
        if (tenantId == null || conversationId == null) {
            return;
        }
        conversationSessionCache.remove(buildConversationSessionKey(tenantId, conversationId));
    }

    public List<WeKnoraChunk> hybridSearch(String query, String knowledgeBaseId, String tenantApiKey) {
        if (!isEnabled()) return Collections.emptyList();

        String url = config.getBaseUrl() + "/knowledge-bases/" + knowledgeBaseId + "/hybrid-search";

        HttpHeaders headers = createHeaders(tenantApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query_text", query);
        requestBody.put("vector_threshold", config.getSearch().getVectorThreshold());
        requestBody.put("match_count", config.getSearch().getMatchCount());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("success") && root.get("success").asBoolean() && root.has("data")) {
                    return objectMapper.convertValue(root.get("data"), new TypeReference<List<WeKnoraChunk>>() {});
                }
            }
            log.warn("WeKnora 混合搜索返回空结果: {}", response.getBody());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("WeKnora 混合搜索异常: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // ==================== 知识详情 ====================

    public WeKnoraKnowledge getKnowledgeDetail(String weKnoraKnowledgeId, String tenantApiKey) {
        if (!isEnabled() || weKnoraKnowledgeId == null) return null;

        String url = config.getBaseUrl() + "/knowledge/" + weKnoraKnowledgeId;
        HttpEntity<?> requestEntity = new HttpEntity<>(createHeaders(tenantApiKey));

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("success") && root.get("success").asBoolean() && root.has("data")) {
                    return objectMapper.treeToValue(root.get("data"), WeKnoraKnowledge.class);
                }
            }
            return null;
        } catch (Exception e) {
            log.error("WeKnora 获取知识详情异常: {}", e.getMessage(), e);
            return null;
        }
    }

    private String getOrCreateConversationSession(Long tenantId, Long conversationId, TenantWeKnoraContext ctx) {
        String cacheKey = buildConversationSessionKey(tenantId, conversationId);
        String cachedSessionId = conversationSessionCache.get(cacheKey);
        if (StrUtil.isNotBlank(cachedSessionId)) {
            log.debug("复用RAG会话: cacheKey={}, sessionId={}", cacheKey, cachedSessionId);
            return cachedSessionId;
        }

        synchronized (conversationSessionCache) {
            cachedSessionId = conversationSessionCache.get(cacheKey);
            if (StrUtil.isNotBlank(cachedSessionId)) {
                log.debug("复用RAG会话(并发命中缓存): cacheKey={}, sessionId={}", cacheKey, cachedSessionId);
                return cachedSessionId;
            }
            String sessionId = createConversationSession(ctx.getKnowledgeBaseId(), ctx.getApiKey());
            conversationSessionCache.put(cacheKey, sessionId);
            log.debug("创建并缓存RAG会话: cacheKey={}, sessionId={}, kbId={}", cacheKey, sessionId, ctx.getKnowledgeBaseId());
            return sessionId;
        }
    }

    private String createConversationSession(String knowledgeBaseId, String tenantApiKey) {
        String url = config.getBaseUrl() + "/sessions";

        HttpHeaders headers = createHeaders(tenantApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("knowledge_base_id", knowledgeBaseId);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data");
                String sessionId = data.path("id").asText(null);
                if (StrUtil.isBlank(sessionId)) {
                    sessionId = root.path("id").asText(null);
                }
                if (StrUtil.isNotBlank(sessionId)) {
                    return sessionId;
                }
            }
            throw new RuntimeException("WeKnora 创建会话失败: " + response.getBody());
        } catch (Exception e) {
            log.error("WeKnora 创建会话异常: {}", e.getMessage(), e);
            throw new RuntimeException("WeKnora 创建会话失败: " + e.getMessage(), e);
        }
    }

    private WeKnoraChatResult parseChatResult(String responseBody) {
        if (StrUtil.isBlank(responseBody)) {
            return new WeKnoraChatResult("", Collections.emptyList(), false);
        }

        String answer = "";
        List<WeKnoraChunk> references = new ArrayList<>();
        boolean completed = false;

        try {
            String trimmed = responseBody.trim();
            if (trimmed.startsWith("{")) {
                JsonNode root = objectMapper.readTree(trimmed);
                answer = appendAnswer(answer, root);
                references.addAll(extractReferences(root));
                completed = root.path("done").asBoolean(false);
            } else {
                String[] events = responseBody.split("\\r?\\n\\r?\\n");
                for (String event : events) {
                    String payload = Arrays.stream(event.split("\\r?\\n"))
                            .map(String::trim)
                            .filter(line -> line.startsWith("data:"))
                            .map(line -> line.substring(5).trim())
                            .filter(StrUtil::isNotBlank)
                            .collect(Collectors.joining("\n"));

                    if (StrUtil.isBlank(payload) || "[DONE]".equalsIgnoreCase(payload)) {
                        continue;
                    }

                    JsonNode node = objectMapper.readTree(payload);
                    if ("error".equalsIgnoreCase(node.path("response_type").asText())) {
                        throw new RuntimeException(node.path("content").asText("WeKnora 问答失败"));
                    }

                    answer = appendAnswer(answer, node);
                    references.addAll(extractReferences(node));
                    if (node.path("done").asBoolean(false)) {
                        completed = true;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析 WeKnora 问答结果失败: {}", e.getMessage());
            return new WeKnoraChatResult("", Collections.emptyList(), false);
        }

        List<WeKnoraChunk> uniqueReferences = references.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                chunk -> String.format("%s:%s:%s",
                                        StrUtil.blankToDefault(chunk.getKnowledgeId(), ""),
                                        chunk.getChunkIndex(),
                                        chunk.getStartAt()),
                                chunk -> chunk,
                                (left, right) -> left,
                                LinkedHashMap::new),
                        map -> new ArrayList<>(map.values())
                ));

        return new WeKnoraChatResult(answer.trim(), uniqueReferences, completed);
    }

    private WeKnoraChatResult executeKnowledgeChat(TenantWeKnoraContext ctx, String sessionId,
                                                   String query, List<String> knowledgeIds,
                                                   boolean useAgentChat) {
        String url = config.getBaseUrl() + (useAgentChat ? "/agent-chat/" : "/knowledge-chat/") + sessionId;
        log.debug("调用RAG问答接口: url={}, kbId={}, useAgentChat={}, knowledgeIds={}, query={}",
                url, ctx.getKnowledgeBaseId(), useAgentChat, knowledgeIds, abbreviateForLog(query));

        HttpHeaders headers = createHeaders(ctx.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON));

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("query", query);
        if (useAgentChat) {
            requestBody.put("knowledge_base_ids", List.of(ctx.getKnowledgeBaseId()));
            requestBody.put("knowledge_ids", knowledgeIds);
            requestBody.put("agent_enabled", false);
            requestBody.put("web_search_enabled", false);
        }

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("WeKnora 问答返回异常: " + response.getStatusCode());
        }
        return parseChatResult(response.getBody());
    }

    private boolean isUnavailableChatResult(WeKnoraChatResult result) {
        if (result == null || StrUtil.isBlank(result.getAnswer())) {
            return true;
        }
        return result.getAnswer().contains("NO_MATCH");
    }

    private String abbreviateForLog(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() > 120 ? normalized.substring(0, 120) + "..." : normalized;
    }

    private String appendAnswer(String currentAnswer, JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return currentAnswer;
        }

        String responseType = node.path("response_type").asText("");
        String content = node.path("content").asText("");

        if (StrUtil.isBlank(content)) {
            JsonNode data = node.path("data");
            if (!data.isMissingNode()) {
                content = data.path("content").asText("");
                if (StrUtil.isBlank(content)) {
                    content = data.path("answer").asText("");
                }
            }
        }

        if (StrUtil.isBlank(content)) {
            return currentAnswer;
        }

        if (StrUtil.isBlank(responseType)
                || "answer".equalsIgnoreCase(responseType)
                || "final".equalsIgnoreCase(responseType)
                || "message".equalsIgnoreCase(responseType)) {
            return currentAnswer + content;
        }
        return currentAnswer;
    }

    private List<WeKnoraChunk> extractReferences(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return Collections.emptyList();
        }

        JsonNode referenceNode = node.get("knowledge_references");
        if ((referenceNode == null || referenceNode.isMissingNode() || referenceNode.isNull())
                && node.has("data")) {
            referenceNode = node.path("data").get("knowledge_references");
        }

        if (referenceNode != null && referenceNode.isArray()) {
            return objectMapper.convertValue(referenceNode, new TypeReference<List<WeKnoraChunk>>() {});
        }
        return Collections.emptyList();
    }

    private String buildConversationSessionKey(Long tenantId, Long conversationId) {
        return tenantId + ":" + (conversationId != null ? conversationId : 0L);
    }

    private HttpHeaders createHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        if (StrUtil.isNotBlank(apiKey)) {
            headers.set("X-API-Key", apiKey);
        }
        return headers;
    }
}

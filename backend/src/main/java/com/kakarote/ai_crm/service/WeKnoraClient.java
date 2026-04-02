package com.kakarote.ai_crm.service;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.config.WeKnoraConfig;
import com.kakarote.ai_crm.entity.VO.WeKnoraChunk;
import com.kakarote.ai_crm.entity.VO.WeKnoraKnowledge;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * WeKnora REST 客户端。
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
    private final ConcurrentHashMap<String, String> conversationSessionCache = new ConcurrentHashMap<>();

    public WeKnoraClient(WeKnoraConfig config) {
        this(config, new RestTemplate(), new ObjectMapper());
    }

    WeKnoraClient(WeKnoraConfig config, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.config = config;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Data
    public static class WeKnoraChatResult {
        private final String answer;
        private final List<WeKnoraChunk> references;
        private final boolean completed;
    }

    @Data
    public static class WeKnoraPreviewResult {
        private final byte[] body;
        private final MediaType contentType;
        private final String contentDisposition;
    }

    public boolean isSupportedFileType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return false;
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return SUPPORTED_EXTENSIONS.contains(ext);
    }

    public Set<String> getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }

    public boolean isEnabled() {
        return config.isEnabled()
                && StrUtil.isNotBlank(config.getApiKey())
                && StrUtil.isNotBlank(config.getKnowledgeBaseId());
    }

    public WeKnoraKnowledge uploadDocument(MultipartFile file) throws IOException {
        return uploadDocument(file.getBytes(), file.getOriginalFilename());
    }

    public WeKnoraKnowledge uploadDocument(String filePath, String originalFilename) throws IOException {
        File file = new File(filePath);
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        return uploadDocument(fileBytes, originalFilename);
    }

    public WeKnoraKnowledge uploadDocument(String filePath, String originalFilename,
                                           String knowledgeBaseId, String apiKey) throws IOException {
        File file = new File(filePath);
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        return uploadDocument(fileBytes, originalFilename, knowledgeBaseId, apiKey);
    }

    public WeKnoraKnowledge uploadDocument(byte[] fileBytes, String originalFilename) throws IOException {
        return uploadDocument(fileBytes, originalFilename, config.getKnowledgeBaseId(), config.getApiKey());
    }

    public WeKnoraKnowledge uploadDocument(byte[] fileBytes, String originalFilename,
                                           String knowledgeBaseId, String apiKey) throws IOException {
        if (!isAvailable(knowledgeBaseId, apiKey)) {
            log.debug("WeKnora 未启用，跳过上传");
            return null;
        }

        String url = config.getBaseUrl() + "/knowledge-bases/" + knowledgeBaseId + "/knowledge/file";

        HttpHeaders headers = createHeaders(apiKey);
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

    public void deleteKnowledge(String weKnoraKnowledgeId) {
        deleteKnowledge(weKnoraKnowledgeId, config.getApiKey());
    }

    public void deleteKnowledge(String weKnoraKnowledgeId, String apiKey) {
        if (!isAvailable(config.getKnowledgeBaseId(), apiKey) || weKnoraKnowledgeId == null) {
            return;
        }

        String url = config.getBaseUrl() + "/knowledge/" + weKnoraKnowledgeId;
        HttpEntity<?> requestEntity = new HttpEntity<>(createHeaders(apiKey));

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

    public List<WeKnoraChunk> searchKnowledge(String query) {
        return searchKnowledge(query, config.getKnowledgeBaseId(), config.getApiKey());
    }

    public List<WeKnoraChunk> searchKnowledge(String query, String knowledgeBaseId, String apiKey) {
        if (!isAvailable(knowledgeBaseId, apiKey)) {
            return Collections.emptyList();
        }

        String url = config.getBaseUrl() + "/knowledge-search";

        HttpHeaders headers = createHeaders(apiKey);
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
                            root.get("data"),
                            new TypeReference<List<WeKnoraChunk>>() {
                            }
                    );
                    double threshold = config.getSearch().getVectorThreshold();
                    int maxCount = config.getSearch().getMatchCount();
                    return chunks.stream()
                            .filter(c -> c.getScore() == null || c.getScore() >= threshold)
                            .limit(maxCount)
                            .toList();
                }
            }
            log.warn("WeKnora 搜索返回空结果: {}", response.getBody());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("WeKnora 搜索异常: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<WeKnoraChunk> hybridSearch(String query) {
        return hybridSearch(query, config.getKnowledgeBaseId(), config.getApiKey());
    }

    public List<WeKnoraChunk> hybridSearch(String query, String knowledgeBaseId, String apiKey) {
        if (!isAvailable(knowledgeBaseId, apiKey)) {
            return Collections.emptyList();
        }

        String url = config.getBaseUrl() + "/knowledge-bases/" + knowledgeBaseId + "/hybrid-search";

        HttpHeaders headers = createHeaders(apiKey);
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
                    return objectMapper.convertValue(
                            root.get("data"),
                            new TypeReference<List<WeKnoraChunk>>() {
                            }
                    );
                }
            }
            log.warn("WeKnora 混合搜索返回空结果: {}", response.getBody());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("WeKnora 混合搜索异常: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public WeKnoraKnowledge getKnowledgeDetail(String weKnoraKnowledgeId) {
        return getKnowledgeDetail(weKnoraKnowledgeId, config.getApiKey());
    }

    public WeKnoraKnowledge getKnowledgeDetail(String weKnoraKnowledgeId, String apiKey) {
        if (!isAvailable(config.getKnowledgeBaseId(), apiKey) || weKnoraKnowledgeId == null) {
            return null;
        }

        String url = config.getBaseUrl() + "/knowledge/" + weKnoraKnowledgeId;
        HttpEntity<?> requestEntity = new HttpEntity<>(createHeaders(apiKey));

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

    public WeKnoraPreviewResult getKnowledgePreview(String weKnoraKnowledgeId) {
        return getKnowledgePreview(weKnoraKnowledgeId, config.getApiKey());
    }

    public WeKnoraPreviewResult getKnowledgePreview(String weKnoraKnowledgeId, String apiKey) {
        if (!isAvailable(config.getKnowledgeBaseId(), apiKey) || StrUtil.isBlank(weKnoraKnowledgeId)) {
            return null;
        }

        String url = config.getBaseUrl() + "/knowledge/" + weKnoraKnowledgeId + "/preview";
        HttpEntity<?> requestEntity = new HttpEntity<>(createHeaders(apiKey));

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, byte[].class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("WeKnora preview returned non-success status: {} - {}", weKnoraKnowledgeId, response.getStatusCode());
                return null;
            }
            byte[] body = response.getBody();
            if (body == null || body.length == 0) {
                log.warn("WeKnora preview returned empty body: {}", weKnoraKnowledgeId);
                return null;
            }
            return new WeKnoraPreviewResult(
                    body,
                    response.getHeaders().getContentType(),
                    response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
            );
        } catch (Exception e) {
            log.error("WeKnora preview request failed: {}", e.getMessage(), e);
            return null;
        }
    }

    public WeKnoraChatResult askKnowledgeQuestion(Long conversationId, String query) {
        return askKnowledgeQuestion(conversationId, query, Collections.emptyList());
    }

    public WeKnoraChatResult askKnowledgeQuestion(Long conversationId, String query, List<String> knowledgeIds) {
        if (!isEnabled() || StrUtil.isBlank(query)) {
            return new WeKnoraChatResult("", Collections.emptyList(), false);
        }

        String sessionId = getOrCreateConversationSession(conversationId);
        boolean useAgentChat = knowledgeIds != null && !knowledgeIds.isEmpty();
        log.debug(
                "RAG问答请求开始: conversationId={}, sessionId={}, kbId={}, useAgentChat={}, knowledgeIds={}, query={}",
                conversationId,
                sessionId,
                config.getKnowledgeBaseId(),
                useAgentChat,
                knowledgeIds,
                abbreviateForLog(query)
        );
        try {
            WeKnoraChatResult result = executeKnowledgeChat(sessionId, query, knowledgeIds, useAgentChat);
            if (useAgentChat && isUnavailableChatResult(result)) {
                log.warn("WeKnora 定向知识问答未返回可用结果，降级为通用知识库问答: sessionId={}", sessionId);
                result = executeKnowledgeChat(sessionId, query, Collections.emptyList(), false);
            }
            log.debug(
                    "RAG问答请求完成: conversationId={}, sessionId={}, answerLength={}, references={}, completed={}",
                    conversationId,
                    sessionId,
                    result.getAnswer() != null ? result.getAnswer().length() : 0,
                    result.getReferences() != null ? result.getReferences().size() : 0,
                    result.isCompleted()
            );
            return result;
        } catch (Exception e) {
            log.error("WeKnora 问答异常: sessionId={}, error={}", sessionId, e.getMessage(), e);
            return new WeKnoraChatResult("", Collections.emptyList(), false);
        }
    }

    public void clearConversationSession(Long conversationId) {
        if (conversationId == null) {
            return;
        }
        conversationSessionCache.remove(buildConversationSessionKey(conversationId));
    }

    private boolean isAvailable(String knowledgeBaseId, String apiKey) {
        return config.isEnabled()
                && StrUtil.isNotBlank(apiKey)
                && StrUtil.isNotBlank(knowledgeBaseId);
    }

    private String getOrCreateConversationSession(Long conversationId) {
        String cacheKey = buildConversationSessionKey(conversationId);
        String cachedSessionId = conversationSessionCache.get(cacheKey);
        if (StrUtil.isNotBlank(cachedSessionId)) {
            return cachedSessionId;
        }

        synchronized (conversationSessionCache) {
            cachedSessionId = conversationSessionCache.get(cacheKey);
            if (StrUtil.isNotBlank(cachedSessionId)) {
                return cachedSessionId;
            }

            String sessionId = createConversationSession(config.getKnowledgeBaseId(), config.getApiKey());
            conversationSessionCache.put(cacheKey, sessionId);
            return sessionId;
        }
    }

    private String createConversationSession(String knowledgeBaseId, String apiKey) {
        String url = config.getBaseUrl() + "/sessions";

        HttpHeaders headers = createHeaders(apiKey);
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

    private WeKnoraChatResult executeKnowledgeChat(String sessionId, String query,
                                                   List<String> knowledgeIds, boolean useAgentChat) {
        String url = config.getBaseUrl() + (useAgentChat ? "/agent-chat/" : "/knowledge-chat/") + sessionId;

        HttpHeaders headers = createHeaders(config.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON));

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("query", query);
        if (useAgentChat) {
            requestBody.put("knowledge_base_ids", List.of(config.getKnowledgeBaseId()));
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
                                chunk -> String.format(
                                        "%s:%s:%s",
                                        StrUtil.blankToDefault(chunk.getKnowledgeId(), ""),
                                        chunk.getChunkIndex(),
                                        chunk.getStartAt()
                                ),
                                chunk -> chunk,
                                (left, right) -> left,
                                LinkedHashMap::new
                        ),
                        map -> new ArrayList<>(map.values())
                ));

        return new WeKnoraChatResult(answer.trim(), uniqueReferences, completed);
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
        if ((referenceNode == null || referenceNode.isMissingNode() || referenceNode.isNull()) && node.has("data")) {
            referenceNode = node.path("data").get("knowledge_references");
        }

        if (referenceNode != null && referenceNode.isArray()) {
            return objectMapper.convertValue(referenceNode, new TypeReference<List<WeKnoraChunk>>() {
            });
        }
        return Collections.emptyList();
    }

    private boolean isUnavailableChatResult(WeKnoraChatResult result) {
        if (result == null || StrUtil.isBlank(result.getAnswer())) {
            return true;
        }
        return result.getAnswer().contains("NO_MATCH");
    }

    private String buildConversationSessionKey(Long conversationId) {
        return String.valueOf(conversationId != null ? conversationId : 0L);
    }

    private String abbreviateForLog(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() > 120 ? normalized.substring(0, 120) + "..." : normalized;
    }

    private HttpHeaders createHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        if (StrUtil.isNotBlank(apiKey)) {
            headers.set("X-API-Key", apiKey);
        }
        return headers;
    }
}

package com.kakarote.ai_crm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.config.WeKnoraConfig;
import com.kakarote.ai_crm.entity.VO.WeKnoraChunk;
import com.kakarote.ai_crm.entity.VO.WeKnoraKnowledge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * WeKnora REST 客户端
 * 封装与 WeKnora 知识库服务的 HTTP 交互
 */
@Slf4j
@Service
public class WeKnoraClient {

    /**
     * WeKnora 支持的文件扩展名（小写）
     */
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "txt", "md", "markdown",
            "xls", "xlsx", "csv",
            "html", "htm", "json", "xml"
    );

    private final RestTemplate restTemplate;
    private final WeKnoraConfig config;
    private final ObjectMapper objectMapper;

    public WeKnoraClient(WeKnoraConfig config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 检查文件类型是否被 WeKnora 支持
     *
     * @param filename 文件名
     * @return 是否支持
     */
    public boolean isSupportedFileType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return false;
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return SUPPORTED_EXTENSIONS.contains(ext);
    }

    /**
     * 获取支持的文件扩展名列表（用于前端提示）
     */
    public Set<String> getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }

    /**
     * 检查 WeKnora 是否启用
     */
    public boolean isEnabled() {
        return config.isEnabled() && config.getApiKey() != null && !config.getApiKey().isEmpty();
    }

    /**
     * 上传文档到 WeKnora
     *
     * @param file 文件
     * @return WeKnora 中的知识信息
     */
    public WeKnoraKnowledge uploadDocument(MultipartFile file) throws IOException {
        return uploadDocument(file.getBytes(), file.getOriginalFilename());
    }

    /**
     * 上传文档到 WeKnora（从文件路径）
     *
     * @param filePath 文件路径
     * @param originalFilename 原始文件名
     * @return WeKnora 中的知识信息
     */
    public WeKnoraKnowledge uploadDocument(String filePath, String originalFilename) throws IOException {
        File file = new File(filePath);
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        return uploadDocument(fileBytes, originalFilename);
    }

    /**
     * 上传文档到 WeKnora（从字节数组）
     *
     * @param fileBytes 文件字节数组
     * @param originalFilename 原始文件名
     * @return WeKnora 中的知识信息
     */
    public WeKnoraKnowledge uploadDocument(byte[] fileBytes, String originalFilename) throws IOException {
        if (!isEnabled()) {
            log.debug("WeKnora 未启用，跳过上传");
            return null;
        }

        String url = config.getBaseUrl() + "/knowledge-bases/" + config.getKnowledgeBaseId() + "/knowledge/file";

        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // 构建 multipart 请求
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
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("success") && root.get("success").asBoolean() && root.has("data")) {
                    return objectMapper.treeToValue(root.get("data"), WeKnoraKnowledge.class);
                }
            }
            log.error("WeKnora 上传失败: {}", response.getBody());
            return null;
        } catch (org.springframework.web.client.HttpClientErrorException.Conflict e) {
            // 409 Conflict - 文件已存在，解析返回的已有知识信息
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

    /**
     * 删除 WeKnora 中的知识
     *
     * @param weKnoraKnowledgeId WeKnora 知识 ID
     */
    public void deleteKnowledge(String weKnoraKnowledgeId) {
        if (!isEnabled() || weKnoraKnowledgeId == null) {
            return;
        }

        String url = config.getBaseUrl() + "/knowledge/" + weKnoraKnowledgeId;

        HttpHeaders headers = createHeaders();
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("WeKnora 知识删除成功: {}", weKnoraKnowledgeId);
            } else {
                log.warn("WeKnora 知识删除失败: {} - {}", weKnoraKnowledgeId, response.getBody());
            }
        } catch (Exception e) {
            log.error("WeKnora 删除异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 语义搜索 - 在知识库中搜索相关文档片段
     *
     * @param query 搜索查询
     * @return 相关文档片段列表
     */
    public List<WeKnoraChunk> searchKnowledge(String query) {
        if (!isEnabled()) {
            log.debug("WeKnora 未启用，跳过搜索");
            return Collections.emptyList();
        }

        String url = config.getBaseUrl() + "/knowledge-search";

        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", query);
        requestBody.put("knowledge_base_id", config.getKnowledgeBaseId());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("success") && root.get("success").asBoolean() && root.has("data")) {
                    List<WeKnoraChunk> chunks = objectMapper.convertValue(
                            root.get("data"),
                            new TypeReference<List<WeKnoraChunk>>() {}
                    );
                    // 按阈值过滤
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

    /**
     * 混合搜索 - 向量 + 关键词
     *
     * @param query 搜索查询
     * @return 相关文档片段列表
     */
    public List<WeKnoraChunk> hybridSearch(String query) {
        if (!isEnabled()) {
            log.debug("WeKnora 未启用，跳过混合搜索");
            return Collections.emptyList();
        }

        String url = config.getBaseUrl() + "/knowledge-bases/" + config.getKnowledgeBaseId() + "/hybrid-search";

        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query_text", query);
        requestBody.put("vector_threshold", config.getSearch().getVectorThreshold());
        requestBody.put("match_count", config.getSearch().getMatchCount());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // WeKnora hybrid-search 使用 GET 但带 body，用 exchange 发送
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("success") && root.get("success").asBoolean() && root.has("data")) {
                    return objectMapper.convertValue(
                            root.get("data"),
                            new TypeReference<List<WeKnoraChunk>>() {}
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

    /**
     * 获取知识详情
     *
     * @param weKnoraKnowledgeId WeKnora 知识 ID
     * @return 知识详情
     */
    public WeKnoraKnowledge getKnowledgeDetail(String weKnoraKnowledgeId) {
        if (!isEnabled() || weKnoraKnowledgeId == null) {
            return null;
        }

        String url = config.getBaseUrl() + "/knowledge/" + weKnoraKnowledgeId;

        HttpHeaders headers = createHeaders();
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

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

    /**
     * 创建带认证的请求头
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", config.getApiKey());
        return headers;
    }
}

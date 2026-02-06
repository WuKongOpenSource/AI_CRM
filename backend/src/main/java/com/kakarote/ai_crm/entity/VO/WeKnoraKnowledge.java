package com.kakarote.ai_crm.entity.VO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * WeKnora 知识实体
 * 对应 WeKnora API 返回的知识（文档）信息
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeKnoraKnowledge {

    /**
     * 知识 ID（WeKnora 中的唯一标识）
     */
    private String id;

    /**
     * 租户 ID
     */
    @JsonProperty("tenant_id")
    private Long tenantId;

    /**
     * 所属知识库 ID
     */
    @JsonProperty("knowledge_base_id")
    private String knowledgeBaseId;

    /**
     * 类型: file / url / manual
     */
    private String type;

    /**
     * 标题
     */
    private String title;

    /**
     * 描述（AI 生成的摘要）
     */
    private String description;

    /**
     * 来源（URL 类型时的原始地址）
     */
    private String source;

    /**
     * 解析状态: pending / processing / completed / failed
     */
    @JsonProperty("parse_status")
    private String parseStatus;

    /**
     * 启用状态: enabled / disabled
     */
    @JsonProperty("enable_status")
    private String enableStatus;

    /**
     * 嵌入模型 ID
     */
    @JsonProperty("embedding_model_id")
    private String embeddingModelId;

    /**
     * 文件名
     */
    @JsonProperty("file_name")
    private String fileName;

    /**
     * 文件类型
     */
    @JsonProperty("file_type")
    private String fileType;

    /**
     * 文件大小（字节）
     */
    @JsonProperty("file_size")
    private Long fileSize;

    /**
     * 文件哈希
     */
    @JsonProperty("file_hash")
    private String fileHash;

    /**
     * 文件路径
     */
    @JsonProperty("file_path")
    private String filePath;

    /**
     * 存储大小
     */
    @JsonProperty("storage_size")
    private Long storageSize;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    /**
     * 创建时间
     */
    @JsonProperty("created_at")
    private String createdAt;

    /**
     * 更新时间
     */
    @JsonProperty("updated_at")
    private String updatedAt;

    /**
     * 处理完成时间
     */
    @JsonProperty("processed_at")
    private String processedAt;

    /**
     * 错误信息
     */
    @JsonProperty("error_message")
    private String errorMessage;
}

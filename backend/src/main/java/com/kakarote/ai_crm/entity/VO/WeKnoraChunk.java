package com.kakarote.ai_crm.entity.VO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * WeKnora 搜索结果片段
 * 对应 WeKnora API 返回的文档分块信息
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeKnoraChunk {

    /**
     * 分块 ID
     */
    private String id;

    /**
     * 分块内容（文档片段文本）
     */
    private String content;

    /**
     * 所属知识 ID
     */
    @JsonProperty("knowledge_id")
    private String knowledgeId;

    /**
     * 分块在文档中的索引
     */
    @JsonProperty("chunk_index")
    private Integer chunkIndex;

    /**
     * 知识标题（文档名）
     */
    @JsonProperty("knowledge_title")
    private String knowledgeTitle;

    /**
     * 内容起始位置
     */
    @JsonProperty("start_at")
    private Integer startAt;

    /**
     * 内容结束位置
     */
    @JsonProperty("end_at")
    private Integer endAt;

    /**
     * 序号
     */
    private Integer seq;

    /**
     * 相关性分数 (0-1 或更高，取决于检索方式)
     */
    private Double score;

    /**
     * 分块类型: text / summary / image
     */
    @JsonProperty("chunk_type")
    private String chunkType;

    /**
     * 图片信息（如果是图片分块）
     */
    @JsonProperty("image_info")
    private String imageInfo;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    /**
     * 知识文件名
     */
    @JsonProperty("knowledge_filename")
    private String knowledgeFilename;

    /**
     * 知识来源
     */
    @JsonProperty("knowledge_source")
    private String knowledgeSource;
}

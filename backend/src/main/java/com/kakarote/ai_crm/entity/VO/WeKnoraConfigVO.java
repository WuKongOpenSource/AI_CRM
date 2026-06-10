package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * WeKnora 配置信息
 */
@Data
@Schema(name = "WeKnoraConfigVO", description = "WeKnora配置信息")
public class WeKnoraConfigVO implements Serializable {

    @Schema(description = "是否启用WeKnora")
    private Boolean enabled;

    @Schema(description = "API基础URL")
    private String baseUrl;

    @Schema(description = "API Key（脱敏显示）")
    private String apiKey;

    @Schema(description = "知识库ID")
    private String knowledgeBaseId;

    @Schema(description = "默认知识库名称")
    private String knowledgeBaseName;

    @Schema(description = "LLM服务商")
    private String llmProvider;

    @Schema(description = "LLM模型名称")
    private String llmModelName;

    @Schema(description = "LLM基础URL")
    private String llmBaseUrl;

    @Schema(description = "LLM API Key（脱敏显示）")
    private String llmApiKey;

    @Schema(description = "LLM模型ID")
    private String llmModelId;

    @Schema(description = "Embedding服务商")
    private String embeddingProvider;

    @Schema(description = "Embedding模型名称")
    private String embeddingModelName;

    @Schema(description = "Embedding基础URL")
    private String embeddingBaseUrl;

    @Schema(description = "Embedding API Key（脱敏显示）")
    private String embeddingApiKey;

    @Schema(description = "Embedding向量维度")
    private Integer embeddingDimension;

    @Schema(description = "Embedding模型ID")
    private String embeddingModelId;

    @Schema(description = "最大匹配结果数")
    private Integer matchCount;

    @Schema(description = "向量相似度阈值")
    private Double vectorThreshold;

    @Schema(description = "是否启用自动RAG")
    private Boolean autoRagEnabled;

    @Schema(description = "最后更新时间")
    private Date updateTime;
}

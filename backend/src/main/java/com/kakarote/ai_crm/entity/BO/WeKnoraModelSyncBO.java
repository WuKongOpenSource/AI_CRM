package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "WeKnoraModelSyncBO", description = "WeKnora model sync parameters")
public class WeKnoraModelSyncBO implements Serializable {

    @Schema(description = "Enable WeKnora after sync")
    private Boolean enabled = true;

    @Schema(description = "WeKnora API base URL")
    private String baseUrl;

    @Schema(description = "WeKnora tenant API key")
    private String apiKey;

    @Schema(description = "Default knowledge base name")
    private String knowledgeBaseName;

    @Schema(description = "LLM provider")
    private String llmProvider;

    @Schema(description = "LLM model name")
    private String llmModelName;

    @Schema(description = "LLM base URL")
    private String llmBaseUrl;

    @Schema(description = "LLM API key")
    private String llmApiKey;

    @Schema(description = "Embedding provider")
    private String embeddingProvider;

    @Schema(description = "Embedding model name")
    private String embeddingModelName;

    @Schema(description = "Embedding base URL")
    private String embeddingBaseUrl;

    @Schema(description = "Embedding API key")
    private String embeddingApiKey;

    @Min(value = 1, message = "Embedding dimension must be greater than 0")
    @Schema(description = "Embedding vector dimension")
    private Integer embeddingDimension;
}

package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "WeKnoraModelSyncVO", description = "WeKnora model sync result")
public class WeKnoraModelSyncVO implements Serializable {

    @Schema(description = "Whether the sync succeeded")
    private Boolean success;

    @Schema(description = "Result message")
    private String message;

    @Schema(description = "LLM model ID")
    private String llmModelId;

    @Schema(description = "Embedding model ID")
    private String embeddingModelId;

    @Schema(description = "Default knowledge base ID")
    private String knowledgeBaseId;
}

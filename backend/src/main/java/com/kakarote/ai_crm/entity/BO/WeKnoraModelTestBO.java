package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "WeKnoraModelTestBO", description = "WeKnora model connection test parameters")
public class WeKnoraModelTestBO implements Serializable {

    @Schema(description = "Model type: llm or embedding")
    private String modelType;

    @Schema(description = "Model provider")
    private String provider;

    @Schema(description = "Model name")
    private String modelName;

    @Schema(description = "OpenAI-compatible base URL")
    private String baseUrl;

    @Schema(description = "Provider API key")
    private String apiKey;

    @Schema(description = "Embedding vector dimension")
    private Integer embeddingDimension;
}

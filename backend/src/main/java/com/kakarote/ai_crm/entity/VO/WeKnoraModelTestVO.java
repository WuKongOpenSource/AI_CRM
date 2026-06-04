package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "WeKnoraModelTestVO", description = "WeKnora model connection test result")
public class WeKnoraModelTestVO implements Serializable {

    @Schema(description = "Whether the test succeeded")
    private Boolean success;

    @Schema(description = "Response time in milliseconds")
    private Long responseTime;

    @Schema(description = "Result message")
    private String message;

    @Schema(description = "Model name")
    private String model;

    @Schema(description = "Embedding vector dimension")
    private Integer embeddingDimension;
}

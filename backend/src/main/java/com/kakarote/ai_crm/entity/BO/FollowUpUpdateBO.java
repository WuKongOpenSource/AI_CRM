package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
@Schema(name = "FollowUpUpdateBO", description = "Follow-up update payload")
public class FollowUpUpdateBO {

    @NotNull(message = "Follow-up ID cannot be empty")
    @Schema(description = "Follow-up ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long followUpId;

    @Schema(description = "Relation ID")
    private Long relationId;

    @Schema(description = "Contact ID")
    private Long contactId;

    @NotBlank(message = "Type cannot be empty")
    @Schema(description = "Type: call, meeting, email, visit, other", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @NotBlank(message = "Content cannot be empty")
    @Schema(description = "Follow-up content", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "AI summary")
    private String summary;

    @Schema(description = "Scene type")
    private String sceneType;

    @Schema(description = "AI generated flag")
    private Integer aiGenerated;

    @NotNull(message = "Follow-up time cannot be empty")
    @Schema(description = "Follow-up time", requiredMode = Schema.RequiredMode.REQUIRED)
    private Date followTime;

    @Schema(description = "Next follow-up time")
    private Date nextFollowTime;
}

package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
@Schema(name = "FollowUpUpdateBO", description = "Follow-up update payload")
public class FollowUpUpdateBO {

    @NotNull(message = "跟进ID不能为空")
    @Schema(description = "Follow-up ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long followUpId;

    @Schema(description = "Contact ID")
    private Long contactId;

    @Schema(description = "Relation ID")
    private Long relationId;

    @NotBlank(message = "类型不能为空")
    @Schema(description = "Type: call, meeting, email, visit, other", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @NotBlank(message = "跟进内容不能为空")
    @Schema(description = "Follow-up content", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "AI summary")
    private String summary;

    @Schema(description = "Scene type")
    private String sceneType;

    @Schema(description = "AI generated flag: 0 no, 1 yes")
    private Integer aiGenerated;

    @NotNull(message = "跟进时间不能为空")
    @Schema(description = "Follow-up time", requiredMode = Schema.RequiredMode.REQUIRED)
    private Date followTime;

    @Schema(description = "Next follow-up time")
    private Date nextFollowTime;
}

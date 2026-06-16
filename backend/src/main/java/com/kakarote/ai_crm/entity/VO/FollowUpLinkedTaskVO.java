package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(name = "FollowUpLinkedTaskVO", description = "Task linked to a follow-up")
public class FollowUpLinkedTaskVO {

    @Schema(description = "Task ID")
    private Long taskId;

    @Schema(description = "Task title")
    private String title;

    @Schema(description = "Task description")
    private String description;

    @Schema(description = "Due date")
    private Date dueDate;

    @Schema(description = "Status")
    private String status;

    @Schema(description = "Task type")
    private String taskType;

    @Schema(description = "AI generated flag")
    private Integer generatedByAi;
}

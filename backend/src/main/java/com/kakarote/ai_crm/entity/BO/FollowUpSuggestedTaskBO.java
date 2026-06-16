package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(name = "FollowUpSuggestedTaskBO", description = "Suggested task generated from follow-up")
public class FollowUpSuggestedTaskBO {

    @Schema(description = "Task title")
    private String title;

    @Schema(description = "Task description")
    private String description;

    @Schema(description = "Due date")
    private Date dueDate;

    @Schema(description = "Task type")
    private String taskType;
}

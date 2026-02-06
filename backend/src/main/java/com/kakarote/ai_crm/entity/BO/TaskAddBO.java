package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Date;

/**
 * 任务新增参数
 */
@Data
@Schema(name = "TaskAddBO", description = "任务新增参数")
public class TaskAddBO {

    @NotBlank(message = "标题不能为空")
    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "截止日期")
    private Date dueDate;

    @Schema(description = "优先级: high, medium, low")
    private String priority;

    @Schema(description = "指派人ID")
    private Long assignedTo;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "是否AI生成")
    private Integer generatedByAi;

    @Schema(description = "AI生成上下文")
    private String aiContext;
}

package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

/**
 * 任务更新参数
 */
@Data
@Schema(name = "TaskUpdateBO", description = "任务更新参数")
public class TaskUpdateBO {

    @NotNull(message = "任务ID不能为空")
    @Schema(description = "任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long taskId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "截止日期")
    private Date dueDate;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "指派人ID")
    private Long assignedTo;

    @Schema(description = "客户ID")
    private Long customerId;
}

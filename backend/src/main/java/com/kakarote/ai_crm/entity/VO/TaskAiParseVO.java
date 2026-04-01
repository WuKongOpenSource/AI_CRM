package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "AI解析任务响应")
public class TaskAiParseVO {

    @Schema(description = "任务标题")
    private String title;

    @Schema(description = "截止日期 yyyy-MM-dd HH:mm")
    private String dueDate;

    @Schema(description = "优先级: high/medium/low")
    private String priority;

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "关联客户名称")
    private String customerName;

    @Schema(description = "参与人名称（逗号分隔）")
    private String participantNames;

    @Schema(description = "负责人名称")
    private String assignedToName;

    @Schema(description = "任务描述")
    private String description;
}

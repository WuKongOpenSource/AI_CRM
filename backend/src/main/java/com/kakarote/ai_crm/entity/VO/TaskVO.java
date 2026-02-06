package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 任务视图对象
 */
@Data
@Schema(name = "TaskVO", description = "任务视图对象")
public class TaskVO {

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "截止日期")
    private Date dueDate;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "优先级名称")
    private String priorityName;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "指派人ID")
    private Long assignedTo;

    @Schema(description = "指派人姓名")
    private String assignedToName;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "是否AI生成")
    private Integer generatedByAi;

    @Schema(description = "完成时间")
    private Date completedTime;

    @Schema(description = "创建人ID")
    private Long createUserId;

    @Schema(description = "创建人姓名")
    private String createUserName;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "是否逾期")
    private Boolean overdue;
}

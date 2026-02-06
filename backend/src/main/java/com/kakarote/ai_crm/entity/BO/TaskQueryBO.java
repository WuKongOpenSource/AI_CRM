package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 任务查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "TaskQueryBO", description = "任务查询参数")
public class TaskQueryBO extends PageEntity {

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "指派人ID")
    private Long assignedTo;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "是否AI生成")
    private Integer generatedByAi;

    @Schema(description = "截止日期开始")
    private Date dueDateStart;

    @Schema(description = "截止日期结束")
    private Date dueDateEnd;

    @Schema(description = "筛选条件: today, thisWeek, overdue, all")
    private String filter;
}

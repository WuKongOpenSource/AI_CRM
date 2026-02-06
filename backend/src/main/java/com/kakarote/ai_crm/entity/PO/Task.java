package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 任务表
 */
@Data
@TableName("crm_task")
@Schema(name = "Task", description = "任务表")
public class Task implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "任务ID")
    private Long taskId;

    /**
     * 标题
     */
    @Schema(description = "标题")
    private String title;

    /**
     * 描述
     */
    @Schema(description = "描述")
    private String description;

    /**
     * 截止日期
     */
    @Schema(description = "截止日期")
    private Date dueDate;

    /**
     * 优先级: high, medium, low
     */
    @Schema(description = "优先级: high, medium, low")
    private String priority;

    /**
     * 状态: pending, in_progress, completed
     */
    @Schema(description = "状态: pending, in_progress, completed")
    private String status;

    /**
     * 指派人ID
     */
    @Schema(description = "指派人ID")
    private Long assignedTo;

    /**
     * 关联客户ID
     */
    @Schema(description = "关联客户ID")
    private Long customerId;

    /**
     * 是否AI生成: 0-否, 1-是
     */
    @Schema(description = "是否AI生成: 0-否, 1-是")
    private Integer generatedByAi;

    /**
     * AI生成上下文
     */
    @Schema(description = "AI生成上下文")
    private String aiContext;

    /**
     * 完成时间
     */
    @Schema(description = "完成时间")
    private Date completedTime;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建人ID")
    private Long createUserId;

    /**
     * 修改人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "修改人ID")
    private Long updateUserId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "修改时间")
    private Date updateTime;
}

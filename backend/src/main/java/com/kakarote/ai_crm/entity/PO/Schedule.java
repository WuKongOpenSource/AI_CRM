package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 日程安排表
 */
@Data
@TableName("crm_schedule")
@Schema(name = "Schedule", description = "日程安排表")
public class Schedule implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "日程ID")
    private Long scheduleId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "开始时间")
    private Date startTime;

    @Schema(description = "结束时间")
    private Date endTime;

    @Schema(description = "类型: meeting, call, visit, other")
    private String type;

    @Schema(description = "关联客户ID")
    private Long customerId;

    @Schema(description = "联系人ID")
    private Long contactId;

    @Schema(description = "地点")
    private String location;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建人ID")
    private Long createUserId;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "修改时间")
    private Date updateTime;
}

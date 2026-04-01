package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

/**
 * 日程新增参数
 */
@Data
@Schema(name = "ScheduleAddBO", description = "日程新增参数")
public class ScheduleAddBO {

    @NotBlank(message = "标题不能为空")
    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "描述")
    private String description;

    @NotNull(message = "开始时间不能为空")
    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
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
}

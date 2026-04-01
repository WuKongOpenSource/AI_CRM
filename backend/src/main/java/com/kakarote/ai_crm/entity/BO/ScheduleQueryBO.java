package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 日程查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "ScheduleQueryBO", description = "日程查询参数")
public class ScheduleQueryBO extends PageEntity {

    @Schema(description = "关联客户ID")
    private Long customerId;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "开始日期（范围查询）")
    private Date startDate;

    @Schema(description = "结束日期（范围查询）")
    private Date endDate;
}

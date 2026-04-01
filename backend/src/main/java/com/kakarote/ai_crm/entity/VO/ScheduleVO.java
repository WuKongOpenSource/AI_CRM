package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 日程视图对象
 */
@Data
@Schema(name = "ScheduleVO", description = "日程视图对象")
public class ScheduleVO {

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

    @Schema(description = "类型")
    private String type;

    @Schema(description = "类型名称")
    private String typeName;

    @Schema(description = "关联客户ID")
    private Long customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "联系人ID")
    private Long contactId;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "地点")
    private String location;

    @Schema(description = "创建人ID")
    private Long createUserId;

    @Schema(description = "创建人姓名")
    private String createUserName;

    @Schema(description = "创建时间")
    private Date createTime;
}

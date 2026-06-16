package com.kakarote.ai_crm.entity.VO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

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

    @Schema(description = "关系人ID")
    private Long relationId;

    @Schema(description = "关系人姓名")
    private String relationName;

    @Schema(description = "联系人ID")
    private Long contactId;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "地点")
    private String location;

    @JsonIgnore
    @Schema(description = "参与人员工ID（逗号分隔）", hidden = true)
    private String participantUserIdsText;

    @Schema(description = "参与人名称（逗号分隔）")
    private String participantNames;

    @Schema(description = "参与人员工ID列表")
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> participantUserIds;

    @Schema(description = "参与人员工")
    private List<ScheduleParticipantUserVO> participantUsers;

    @Schema(description = "创建人ID")
    private Long createUserId;

    @Schema(description = "创建人姓名")
    private String createUserName;

    @Schema(description = "创建时间")
    private Date createTime;
}

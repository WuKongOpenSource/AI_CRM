package com.kakarote.ai_crm.entity.VO;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "AI 解析日程响应")
public class ScheduleAiParseVO {

    @Schema(description = "日程标题")
    private String title;

    @Schema(description = "开始时间 yyyy-MM-dd HH:mm")
    private String startTime;

    @Schema(description = "结束时间 yyyy-MM-dd HH:mm")
    private String endTime;

    @Schema(description = "日程类型 meeting/call/visit/other")
    private String type;

    @Schema(description = "关联客户名称")
    private String customerName;

    @Schema(description = "参与人名称（逗号分隔）")
    private String participantNames;

    @Schema(description = "已匹配的参与人员工ID")
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> participantUserIds;

    @Schema(description = "已匹配的参与人员工")
    private List<ScheduleParticipantUserVO> participantUsers;

    @Schema(description = "未匹配的参与人名称（逗号分隔）")
    private String unmatchedParticipantNames;

    @Schema(description = "地点")
    private String location;

    @Schema(description = "日程描述")
    private String description;
}

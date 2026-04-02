package com.kakarote.ai_crm.entity.VO;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "日程参与员工")
public class ScheduleParticipantUserVO {

    @Schema(description = "员工ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @Schema(description = "真实姓名")
    private String realname;

    @Schema(description = "用户名")
    private String username;
}

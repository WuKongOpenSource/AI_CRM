package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "RelationDetailVO", description = "Relation detail")
public class RelationDetailVO {

    @Schema(description = "Base relation information")
    private RelationVO relation;

    @Schema(description = "Related tasks")
    private List<TaskVO> tasks;

    @Schema(description = "Related schedules")
    private List<ScheduleVO> schedules;

    @Schema(description = "Related attachments")
    private List<KnowledgeVO> attachments;

    @Schema(description = "History records")
    private List<FollowUpVO> histories;
}

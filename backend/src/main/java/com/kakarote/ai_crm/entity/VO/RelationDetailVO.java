package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 关系人详情视图对象。
 */
@Data
@Schema(name = "RelationDetailVO", description = "关系人详情视图对象")
public class RelationDetailVO {

    @Schema(description = "基本信息")
    private RelationVO relation;

    @Schema(description = "相关任务")
    private List<TaskVO> tasks;

    @Schema(description = "相关日程")
    private List<ScheduleVO> schedules;

    @Schema(description = "相关附件")
    private List<KnowledgeVO> attachments;

    @Schema(description = "历史记录")
    private List<FollowUpVO> histories;
}

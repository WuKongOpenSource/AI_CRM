package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "CandidateQueryBO", description = "候选人查询参数")
public class CandidateQueryBO extends PageEntity {

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "阶段")
    private String stage;

    @Schema(description = "阶段列表")
    private List<String> stages;

    @Schema(description = "负责人ID")
    private Long ownerId;

    @Schema(description = "来源")
    private String source;

    @Schema(description = "应聘岗位")
    private String appliedPosition;

    @Schema(description = "关联招聘岗位ID")
    private Long recruitmentJobId;

    @Schema(description = "创建开始时间")
    private Date createTimeStart;

    @Schema(description = "创建结束时间")
    private Date createTimeEnd;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "排序字段")
    private String sortBy;

    @Schema(description = "排序方向")
    private String sortOrder;

    @Schema(hidden = true)
    private Boolean allData;

    @Schema(hidden = true)
    private List<Long> userIds;
}

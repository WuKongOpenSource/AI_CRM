package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 浠诲姟鏌ヨ鍙傛暟
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "TaskQueryBO", description = "浠诲姟鏌ヨ鍙傛暟")
public class TaskQueryBO extends PageEntity {

    @Schema(description = "浠诲姟ID")
    private Long taskId;

    @Schema(description = "鍏抽敭璇?")
    private String keyword;

    @Schema(description = "鐘舵€?")
    private String status;

    @Schema(description = "浼樺厛绾?")
    private String priority;

    @Schema(description = "鎸囨淳浜篒D")
    private Long assignedTo;

    @Schema(description = "瀹㈡埛ID")
    private Long customerId;

    @Schema(description = "关系人ID")
    private Long relationId;

    @Schema(description = "所属项目ID")
    private Long projectId;

    @Schema(description = "所属项目泳道ID")
    private Long laneId;

    @Schema(description = "鏄惁AI鐢熸垚")
    private Integer generatedByAi;

    @Schema(description = "鎴鏃ユ湡寮€濮?")
    private Date dueDateStart;

    @Schema(description = "鎴鏃ユ湡缁撴潫")
    private Date dueDateEnd;

    @Schema(description = "绛涢€夋潯浠? today, thisWeek, overdue, all")
    private String filter;

    @Schema(description = "Sort mode: default/value")
    private String sortMode;

    @Schema(description = "Only return high-value tasks when value sorting is enabled")
    private Boolean highValueOnly;
}

package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * 鏃ョ▼鏌ヨ鍙傛暟
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "ScheduleQueryBO", description = "鏃ョ▼鏌ヨ鍙傛暟")
public class ScheduleQueryBO extends PageEntity {

    @Schema(description = "鏃ョ▼ID")
    private Long scheduleId;

    @Schema(description = "鍏抽敭璇?")
    private String keyword;

    @Schema(description = "鍏宠仈瀹㈡埛ID")
    private Long customerId;

    @Schema(description = "关系人ID")
    private Long relationId;

    @Schema(description = "参与人员工ID")
    private Long participantUserId;

    @Schema(description = "绫诲瀷")
    private String type;

    @Schema(description = "寮€濮嬫棩鏈燂紙鑼冨洿鏌ヨ锛?")
    private Date startDate;

    @Schema(description = "缁撴潫鏃ユ湡锛堣寖鍥存煡璇級")
    private Date endDate;

    @Schema(hidden = true)
    private Long currentUserId;

    @Schema(hidden = true)
    private Boolean scheduleAllData;

    @Schema(hidden = true)
    private List<Long> scheduleUserIds;
}

package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 跟进记录查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "FollowUpQueryBO", description = "跟进记录查询参数")
public class FollowUpQueryBO extends PageEntity {

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "联系人ID")
    private Long contactId;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "开始时间")
    private Date startTime;

    @Schema(description = "结束时间")
    private Date endTime;
}

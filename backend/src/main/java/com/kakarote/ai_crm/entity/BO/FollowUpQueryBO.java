package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "FollowUpQueryBO", description = "Follow-up query payload")
public class FollowUpQueryBO extends PageEntity {

    @Schema(description = "Customer ID")
    private Long customerId;

    @Schema(description = "Relation ID")
    private Long relationId;

    @Schema(description = "Contact ID")
    private Long contactId;

    @Schema(description = "Type")
    private String type;

    @Schema(description = "Start time")
    private Date startTime;

    @Schema(description = "End time")
    private Date endTime;
}

package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "AiCreditRecordQueryBO", description = "AI credit record query")
public class AiCreditRecordQueryBO extends PageEntity {

    @Schema(description = "AI action name")
    private String actionName;

    @Schema(description = "Model source: system/custom")
    private String modelSource;

    @Schema(description = "Only records with creditsUsed > 0")
    private Boolean chargedOnly;

    @Schema(description = "Create time start")
    private Date startTime;

    @Schema(description = "Create time end")
    private Date endTime;
}

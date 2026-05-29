package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Schema(name = "AiCreditRecordVO", description = "AI credit record")
public class AiCreditRecordVO {

    private Long recordId;

    private Long tenantId;

    private Long userId;

    private String actionName;

    private String modelSource;

    private String billingModelProvider;

    private String billingModelName;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private Integer tokensPerCredit;

    private BigDecimal creditMultiplier;

    private Boolean chargeable;

    private Long creditsUsed;

    private Long giftCreditsUsed;

    private Long purchasedCreditsUsed;

    private Long balanceBefore;

    private Long balanceAfter;

    private String referenceType;

    private Long referenceId;

    private Date createTime;
}

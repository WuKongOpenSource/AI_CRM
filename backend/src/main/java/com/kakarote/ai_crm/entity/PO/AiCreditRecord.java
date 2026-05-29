package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("crm_ai_credit_record")
public class AiCreditRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long recordId;

    @TableField(fill = FieldFill.INSERT)
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

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}

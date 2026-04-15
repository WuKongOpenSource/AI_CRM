package com.kakarote.ai_crm.entity.BO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenPurchaseCreateBO {

    @NotBlank(message = "套餐不能为空")
    private String planId;

    @NotBlank(message = "支付方式不能为空")
    private String paymentChannel;
}

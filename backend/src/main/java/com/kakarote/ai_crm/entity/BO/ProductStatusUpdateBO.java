package com.kakarote.ai_crm.entity.BO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductStatusUpdateBO {

    @NotNull(message = "产品ID不能为空")
    private Long productId;

    @NotBlank(message = "状态不能为空")
    private String status;
}

package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Schema(name = "ProductUpdateBO", description = "产品更新参数")
public class ProductUpdateBO {

    @NotNull(message = "产品ID不能为空")
    private Long productId;

    private String productName;
    private String productCode;
    private String mainImage;
    private Long categoryId;
    private String productType;
    private String unit;
    private BigDecimal standardPrice;
    private BigDecimal costPrice;
    private Long ownerId;
    private String description;
    private Map<String, Object> customFields;
}

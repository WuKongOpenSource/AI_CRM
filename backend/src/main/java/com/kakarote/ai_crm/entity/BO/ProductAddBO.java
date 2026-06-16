package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Schema(name = "ProductAddBO", description = "产品新增参数")
public class ProductAddBO {

    @NotBlank(message = "产品名称不能为空")
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

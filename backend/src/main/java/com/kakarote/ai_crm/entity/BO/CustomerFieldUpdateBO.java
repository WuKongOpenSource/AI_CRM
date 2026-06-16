package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Single customer field update payload.
 */
@Data
@Schema(name = "CustomerFieldUpdateBO", description = "Single customer field update payload")
public class CustomerFieldUpdateBO {

    @NotNull(message = "customerId cannot be null")
    @Schema(description = "Customer ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long customerId;

    @NotBlank(message = "fieldName cannot be blank")
    @Schema(description = "Field name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fieldName;

    @Schema(description = "Field source: system, custom, contact")
    private String fieldSource;

    @Schema(description = "Field value")
    private Object value;
}

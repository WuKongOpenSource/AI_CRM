package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "CustomFieldUniqueCheckBO", description = "Unique field value check payload")
public class CustomFieldUniqueCheckBO {

    @NotBlank(message = "实体类型不能为空")
    @Schema(description = "实体类型: customer, contact", requiredMode = Schema.RequiredMode.REQUIRED)
    private String entityType;

    @Schema(description = "当前实体ID，编辑时传入用于排除当前记录")
    private Long entityId;

    @NotBlank(message = "字段标识不能为空")
    @Schema(description = "字段标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fieldName;

    @Schema(description = "字段值")
    private Object value;
}

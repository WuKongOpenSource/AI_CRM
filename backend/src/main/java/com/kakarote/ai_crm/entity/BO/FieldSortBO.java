package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 字段排序请求对象
 */
@Data
@Schema(name = "FieldSortBO", description = "字段排序请求对象")
public class FieldSortBO {

    @NotNull(message = "字段ID不能为空")
    @Schema(description = "字段ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fieldId;

    @NotNull(message = "排序序号不能为空")
    @Schema(description = "排序序号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer sortOrder;
}

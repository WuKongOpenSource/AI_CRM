package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 字段选项
 */
@Data
@Schema(name = "FieldOption", description = "字段选项")
public class FieldOption {

    @Schema(description = "选项值")
    private String value;

    @Schema(description = "选项标签")
    private String label;
}

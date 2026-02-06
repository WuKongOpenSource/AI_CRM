package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 字段验证规则
 */
@Data
@Schema(name = "FieldValidation", description = "字段验证规则")
public class FieldValidation {

    @Schema(description = "最小长度")
    private Integer minLength;

    @Schema(description = "最大长度")
    private Integer maxLength;

    @Schema(description = "最小值")
    private BigDecimal min;

    @Schema(description = "最大值")
    private BigDecimal max;

    @Schema(description = "正则表达式")
    private String pattern;

    @Schema(description = "错误提示消息")
    private String message;
}

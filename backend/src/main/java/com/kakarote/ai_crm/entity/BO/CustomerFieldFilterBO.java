package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 客户字段通用筛选条件。
 */
@Data
@Schema(name = "CustomerFieldFilterBO", description = "客户字段通用筛选条件")
public class CustomerFieldFilterBO {

    @Schema(description = "字段名")
    private String fieldName;

    @Schema(description = "字段来源: system/custom，缺省为system")
    private String fieldSource;

    @Schema(description = "操作符: isEmpty/isNotEmpty")
    private String operator;
}

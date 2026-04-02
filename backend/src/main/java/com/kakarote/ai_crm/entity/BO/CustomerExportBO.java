package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 客户导出参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "CustomerExportBO", description = "客户导出参数")
public class CustomerExportBO extends CustomerQueryBO {

    @Schema(description = "指定导出的客户ID列表，为空时按条件导出")
    private List<Long> customerIds;
}

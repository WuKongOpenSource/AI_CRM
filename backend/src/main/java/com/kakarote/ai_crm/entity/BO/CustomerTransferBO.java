package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 客户转移参数
 */
@Data
@Schema(name = "CustomerTransferBO", description = "客户转移参数")
public class CustomerTransferBO {

    @NotEmpty(message = "客户ID列表不能为空")
    @Schema(description = "客户ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> customerIds;

    @NotNull(message = "新负责人ID不能为空")
    @Schema(description = "新负责人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long newOwnerId;
}

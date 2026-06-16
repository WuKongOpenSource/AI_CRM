package com.kakarote.ai_crm.entity.BO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductCategoryMoveBO {

    @NotNull(message = "类目ID不能为空")
    private Long categoryId;

    private Long parentId;
    private Integer sortOrder;
}

package com.kakarote.ai_crm.entity.BO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductCategoryUpdateBO {

    @NotNull(message = "类目ID不能为空")
    private Long categoryId;

    @NotBlank(message = "类目名称不能为空")
    private String categoryName;

    private Integer sortOrder;
}

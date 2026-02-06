package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 更新自定义字段请求对象
 */
@Data
@Schema(name = "CustomFieldUpdateBO", description = "更新自定义字段请求对象")
public class CustomFieldUpdateBO {

    @NotNull(message = "字段ID不能为空")
    @Schema(description = "字段ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fieldId;

    @Schema(description = "字段显示标签(中文)")
    private String fieldLabel;

    @Schema(description = "默认值")
    private String defaultValue;

    @Schema(description = "输入框占位提示")
    private String placeholder;

    @Schema(description = "是否必填")
    private Boolean isRequired;

    @Schema(description = "是否可搜索")
    private Boolean isSearchable;

    @Schema(description = "是否在列表显示")
    private Boolean isShowInList;

    @Schema(description = "选项列表(下拉类型时使用)")
    private List<FieldOption> options;

    @Schema(description = "验证规则")
    private FieldValidation validation;

    @Schema(description = "排序序号")
    private Integer sortOrder;
}

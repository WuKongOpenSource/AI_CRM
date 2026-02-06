package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 添加自定义字段请求对象
 */
@Data
@Schema(name = "CustomFieldAddBO", description = "添加自定义字段请求对象")
public class CustomFieldAddBO {

    @NotBlank(message = "实体类型不能为空")
    @Schema(description = "实体类型: customer, contact", requiredMode = Schema.RequiredMode.REQUIRED)
    private String entityType;

    @NotBlank(message = "字段标识不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "字段标识只能包含字母数字下划线，且以字母开头")
    @Schema(description = "字段标识(英文，用于代码)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fieldName;

    @NotBlank(message = "字段标签不能为空")
    @Schema(description = "字段显示标签(中文)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fieldLabel;

    @NotBlank(message = "字段类型不能为空")
    @Schema(description = "字段类型: text, textarea, number, date, datetime, select, multiselect, checkbox", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fieldType;

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

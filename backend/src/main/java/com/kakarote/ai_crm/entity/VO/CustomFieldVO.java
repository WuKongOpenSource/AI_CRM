package com.kakarote.ai_crm.entity.VO;

import com.kakarote.ai_crm.entity.BO.FieldOption;
import com.kakarote.ai_crm.entity.BO.FieldValidation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 自定义字段视图对象
 */
@Data
@Schema(name = "CustomFieldVO", description = "自定义字段视图对象")
public class CustomFieldVO {

    @Schema(description = "字段ID")
    private Long fieldId;

    @Schema(description = "实体类型: customer, contact")
    private String entityType;

    @Schema(description = "字段标识(英文，用于代码)")
    private String fieldName;

    @Schema(description = "字段显示标签(中文)")
    private String fieldLabel;

    @Schema(description = "字段类型")
    private String fieldType;

    @Schema(description = "实际数据库列名")
    private String columnName;

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

    @Schema(description = "选项列表")
    private List<FieldOption> options;

    @Schema(description = "验证规则")
    private FieldValidation validation;

    @Schema(description = "排序序号")
    private Integer sortOrder;

    @Schema(description = "状态: 0禁用 1启用")
    private Integer status;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "修改时间")
    private Date updateTime;
}

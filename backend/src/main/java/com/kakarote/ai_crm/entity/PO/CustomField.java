package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 自定义字段定义表
 */
@Data
@TableName("crm_custom_field")
@Schema(name = "CustomField", description = "自定义字段定义表")
public class CustomField implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字段ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "字段ID")
    private Long fieldId;

    /**
     * 实体类型: customer, contact
     */
    @Schema(description = "实体类型: customer, contact")
    private String entityType;

    /**
     * 字段标识(英文，用于代码)
     */
    @Schema(description = "字段标识(英文，用于代码)")
    private String fieldName;

    /**
     * 字段显示标签(中文)
     */
    @Schema(description = "字段显示标签(中文)")
    private String fieldLabel;

    /**
     * 字段类型: text, textarea, number, date, datetime, select, multiselect, checkbox
     */
    @Schema(description = "字段类型: text, textarea, number, date, datetime, select, multiselect, checkbox")
    private String fieldType;

    /**
     * 实际数据库列名
     */
    @Schema(description = "实际数据库列名")
    private String columnName;

    /**
     * 数据库列类型
     */
    @Schema(description = "数据库列类型")
    private String columnType;

    /**
     * 默认值
     */
    @Schema(description = "默认值")
    private String defaultValue;

    /**
     * 输入框占位提示
     */
    @Schema(description = "输入框占位提示")
    private String placeholder;

    /**
     * 是否必填: 0否 1是
     */
    @Schema(description = "是否必填: 0否 1是")
    private Integer isRequired;

    /**
     * 是否可搜索: 0否 1是
     */
    @Schema(description = "是否可搜索: 0否 1是")
    private Integer isSearchable;

    /**
     * 是否在列表显示: 0否 1是
     */
    @Schema(description = "是否在列表显示: 0否 1是")
    private Integer isShowInList;

    /**
     * 选项列表(JSON数组): [{"value":"v1","label":"选项1"}]
     */
    @Schema(description = "选项列表(JSON数组)")
    private String options;

    /**
     * 验证规则(JSON): {"min":0,"max":100,"pattern":""}
     */
    @Schema(description = "验证规则(JSON)")
    private String validationRules;

    /**
     * 排序序号
     */
    @Schema(description = "排序序号")
    private Integer sortOrder;

    /**
     * 状态: 0禁用 1启用
     */
    @Schema(description = "状态: 0禁用 1启用")
    private Integer status;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建人ID")
    private Long createUserId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "修改时间")
    private Date updateTime;
}

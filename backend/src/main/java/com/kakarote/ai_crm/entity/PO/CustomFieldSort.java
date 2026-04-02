package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户自定义字段排序与显隐配置
 */
@Data
@TableName("crm_custom_field_sort")
@Schema(description = "用户自定义字段排序配置")
public class CustomFieldSort implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private Long sortId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "实体类型: customer / contact")
    private String entityType;

    @Schema(description = "字段ID，引用 crm_custom_field.field_id")
    private Long fieldId;

    @Schema(description = "排序序号")
    private Integer sortOrder;

    @Schema(description = "是否隐藏: 0=显示, 1=隐藏")
    private Integer isHidden;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}

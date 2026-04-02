package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 自定义字段池（全局共享，跨租户复用物理列）
 */
@Data
@TableName("crm_custom_field_pool")
@Schema(description = "自定义字段池")
public class CustomFieldPool implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private Long poolId;

    @Schema(description = "实体类型: customer / contact")
    private String entityType;

    @Schema(description = "物理列名，如 field_a1b2c3")
    private String columnName;

    @Schema(description = "PostgreSQL列类型，如 VARCHAR(500)")
    private String columnType;

    @Schema(description = "逻辑字段类型：text/number/date等")
    private String fieldType;

    @Schema(description = "物理列是否已创建")
    private Boolean columnCreated;

    @Schema(description = "创建时间")
    private Date createTime;
}

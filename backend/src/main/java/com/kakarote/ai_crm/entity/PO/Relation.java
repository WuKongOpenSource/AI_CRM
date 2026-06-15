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

@Data
@TableName("crm_relation")
@Schema(name = "Relation", description = "External relation")
public class Relation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "Relation ID")
    private Long relationId;

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Avatar object key")
    private String avatar;

    @Schema(description = "Phone")
    private String phone;

    @Schema(description = "Wechat")
    private String wechat;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Relation type")
    private String relationType;

    @Schema(description = "Company")
    private String company;

    @Schema(description = "Linked customer ID")
    private Long customerId;

    @Schema(description = "Remark")
    private String remark;

    @Schema(description = "Source")
    private String source;

    @Schema(description = "Source customer ID")
    private Long sourceCustomerId;

    @Schema(description = "Source contact ID")
    private Long sourceContactId;

    @Schema(description = "Status: 0 disabled, 1 active")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "Create user ID")
    private Long createUserId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "Update user ID")
    private Long updateUserId;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "Create time")
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "Update time")
    private Date updateTime;
}

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
 * 外部关系人表。
 */
@Data
@TableName("crm_relation")
@Schema(name = "Relation", description = "外部关系人表")
public class Relation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "关系人ID")
    private Long relationId;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "微信号")
    private String wechat;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "关系类型")
    private String relationType;

    @Schema(description = "所属公司")
    private String company;

    @Schema(description = "关联客户ID")
    private Long customerId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "来源")
    private String source;

    @Schema(description = "来源客户ID")
    private Long sourceCustomerId;

    @Schema(description = "来源客户联系人ID")
    private Long sourceContactId;

    @Schema(description = "状态: 0-禁用, 1-正常")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建人ID")
    private Long createUserId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "修改人ID")
    private Long updateUserId;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "修改时间")
    private Date updateTime;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "租户ID")
    private Long tenantId;
}

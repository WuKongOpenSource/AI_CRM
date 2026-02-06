package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 联系人表
 */
@Data
@TableName("crm_contact")
@Schema(name = "Contact", description = "联系人表")
public class Contact implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 联系人ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "联系人ID")
    private Long contactId;

    /**
     * 客户ID
     */
    @Schema(description = "客户ID")
    private Long customerId;

    /**
     * 姓名
     */
    @Schema(description = "姓名")
    private String name;

    /**
     * 职位
     */
    @Schema(description = "职位")
    private String position;

    /**
     * 电话
     */
    @Schema(description = "电话")
    private String phone;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱")
    private String email;

    /**
     * 微信
     */
    @Schema(description = "微信")
    private String wechat;

    /**
     * 是否主联系人: 0-否, 1-是
     */
    @Schema(description = "是否主联系人: 0-否, 1-是")
    private Integer isPrimary;

    /**
     * 最后联系时间
     */
    @Schema(description = "最后联系时间")
    private Date lastContactTime;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String notes;

    /**
     * 状态: 0-禁用, 1-正常
     */
    @Schema(description = "状态: 0-禁用, 1-正常")
    private Integer status;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建人ID")
    private Long createUserId;

    /**
     * 修改人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "修改人ID")
    private Long updateUserId;

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

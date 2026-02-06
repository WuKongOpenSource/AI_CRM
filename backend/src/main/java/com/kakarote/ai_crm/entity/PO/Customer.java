package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 客户表
 */
@Data
@TableName("crm_customer")
@Schema(name = "Customer", description = "客户表")
public class Customer implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 客户ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "客户ID")
    private Long customerId;

    /**
     * 公司名称
     */
    @Schema(description = "公司名称")
    private String companyName;

    /**
     * 行业
     */
    @Schema(description = "行业")
    private String industry;

    /**
     * 阶段: lead, qualified, proposal, negotiation, closed, lost
     */
    @Schema(description = "阶段: lead, qualified, proposal, negotiation, closed, lost")
    private String stage;

    /**
     * 负责人ID
     */
    @Schema(description = "负责人ID")
    private Long ownerId;

    /**
     * 客户等级: A, B, C
     */
    @Schema(description = "客户等级: A, B, C")
    private String level;

    /**
     * 客户来源
     */
    @Schema(description = "客户来源")
    private String source;

    /**
     * 地址
     */
    @Schema(description = "地址")
    private String address;

    /**
     * 网站
     */
    @Schema(description = "网站")
    private String website;

    /**
     * 报价金额
     */
    @Schema(description = "报价金额")
    private BigDecimal quotation;

    /**
     * 合同金额
     */
    @Schema(description = "合同金额")
    private BigDecimal contractAmount;

    /**
     * 收入金额
     */
    @Schema(description = "收入金额")
    private BigDecimal revenue;

    /**
     * 最后联系时间
     */
    @Schema(description = "最后联系时间")
    private Date lastContactTime;

    /**
     * 下次跟进时间
     */
    @Schema(description = "下次跟进时间")
    private Date nextFollowTime;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;

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

package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 客户列表视图对象
 */
@Data
@Schema(name = "CustomerListVO", description = "客户列表视图对象")
public class CustomerListVO {

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "行业")
    private String industry;

    @Schema(description = "阶段")
    private String stage;

    @Schema(description = "阶段名称")
    private String stageName;

    @Schema(description = "客户等级")
    private String level;

    @Schema(description = "客户来源")
    private String source;

    @Schema(description = "报价金额")
    private BigDecimal quotation;

    @Schema(description = "合同金额")
    private BigDecimal contractAmount;

    @Schema(description = "收入金额")
    private BigDecimal revenue;

    @Schema(description = "最后联系时间")
    private Date lastContactTime;

    @Schema(description = "下次跟进时间")
    private Date nextFollowTime;

    @Schema(description = "负责人ID")
    private Long ownerId;

    @Schema(description = "负责人姓名")
    private String ownerName;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "主联系人姓名")
    private String primaryContactName;

    @Schema(description = "主联系人电话")
    private String primaryContactPhone;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "自定义字段值")
    private Map<String, Object> customFields;
}

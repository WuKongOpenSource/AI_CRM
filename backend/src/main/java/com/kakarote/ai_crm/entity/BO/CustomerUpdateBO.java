package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 客户更新参数
 */
@Data
@Schema(name = "CustomerUpdateBO", description = "客户更新参数")
public class CustomerUpdateBO {

    @NotNull(message = "客户ID不能为空")
    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long customerId;

    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "行业")
    private String industry;

    @Schema(description = "阶段")
    private String stage;

    @Schema(description = "客户等级")
    private String level;

    @Schema(description = "客户来源")
    private String source;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "网站")
    private String website;

    @Schema(description = "报价金额")
    private BigDecimal quotation;

    @Schema(description = "合同金额")
    private BigDecimal contractAmount;

    @Schema(description = "收入金额")
    private BigDecimal revenue;

    @Schema(description = "下次跟进时间")
    private Date nextFollowTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "自定义字段值")
    private Map<String, Object> customFields;
}

package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 客户新增参数
 */
@Data
@Schema(name = "CustomerAddBO", description = "客户新增参数")
public class CustomerAddBO {

    @NotBlank(message = "公司名称不能为空")
    @Schema(description = "公司名称", requiredMode = Schema.RequiredMode.REQUIRED)
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

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "主联系人姓名")
    private String contactName;

    @Schema(description = "主联系人电话")
    private String contactPhone;

    @Schema(description = "主联系人邮箱")
    private String contactEmail;

    @Schema(description = "主联系人职位")
    private String contactPosition;

    @Schema(description = "自定义字段值")
    private Map<String, Object> customFields;
}

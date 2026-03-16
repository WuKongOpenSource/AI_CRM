package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "AI智能录入客户响应")
public class CustomerAiParseVO {

    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "行业")
    private String industry;

    @Schema(description = "客户级别: A/B/C")
    private String level;

    @Schema(description = "商机阶段: lead/qualified/proposal/negotiation/closed")
    private String stage;

    @Schema(description = "客户来源")
    private String source;

    @Schema(description = "备注信息")
    private String remark;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "联系人电话")
    private String contactPhone;

    @Schema(description = "联系人邮箱")
    private String contactEmail;

    @Schema(description = "联系人职位")
    private String contactPosition;

    @Schema(description = "潜力评分 0-100")
    private Integer score;

    @Schema(description = "自动生成标签")
    private List<String> tags;

    @Schema(description = "AI分析摘要")
    private String summary;

    @Schema(description = "建议下一步行动")
    private String nextStep;

    @Schema(description = "关键要点")
    private List<String> keyPoints;
}

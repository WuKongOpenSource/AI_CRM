package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 客户 AI 分析报告
 */
@Data
@Schema(name = "CustomerAiReportVO", description = "客户 AI 分析报告")
public class CustomerAiReportVO {

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "AI状态探测")
    private String aiStatusDetection;

    @Schema(description = "AI洞察")
    private String aiInsight;

    @Schema(description = "AI深度分析")
    private String aiDeepInsight;

    @Schema(description = "AI建议下一步行动")
    private String aiNextStep;
}
